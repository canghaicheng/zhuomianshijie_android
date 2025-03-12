import * as THREE from 'three';


import {
	GUI
} from 'three/addons/libs/lil-gui.module.min.js';
import {
	OrbitControls
} from 'three/addons/controls/OrbitControls.js';
import {
	Water
} from 'three/addons/objects/Water.js';
import {
	Sky
} from 'three/addons/objects/Sky.js';

import {
	MMDLoader
} from 'three/addons/loaders/MMDLoader.js';

import {
	MMDAnimationHelper
} from 'three/addons/animation/MMDAnimationHelper.js';


import { setupGUI } from '../javascripts/chatHelper.js';

// 在文件顶部添加日志桥接函数
function logToAndroid(message) {
    // 使用fetch或XMLHttpRequest发送日志到后端
    fetch('/log', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ message: message })
    }).catch(err => {
        // 忽略错误，避免日志函数本身产生更多错误
    });
}

// 重写console.log
const originalConsoleLog = console.log;
console.log = function() {
    // 调用原始的console.log保持浏览器控制台输出
    originalConsoleLog.apply(console, arguments);
    
    // 将参数转换为字符串并发送到后端
    const args = Array.from(arguments).map(arg => {
        if (typeof arg === 'object') {
            try {
                return JSON.stringify(arg);
            } catch (e) {
                return String(arg);
            }
        }
        return String(arg);
    });
    
    logToAndroid(args.join(' '));
};

let container, stats;
let cameraState;
let camera, scene, raycaster, renderer;
let orbitControls, water, sun, group, mesh, mouse, click;
let currentModel;
let animationHelper, ikHelper, physicsHelper;
let modelFile;
let animationControls = {};
const defaultModel = '/mmd/wanhua/wanhua.pmx'

console.log("tttttttttttttttttttttttttttttttttttttttttttttttttttttttttt")

// Instantiate a helper
animationHelper = new MMDAnimationHelper({
	resetPhysicsOnLoop: false  // 禁用循环时重置物理
});
const loadingManager = new THREE.LoadingManager();

let vpds = [];
let vmds = [];
const vmdFiles = [
	'vmd/idle.vmd',
	'vmd/twist.vmd',
	'vmd/wavefile_v2.vmd',
	'vmd/catwalk.vmd',
	'vmd/byebyebye.vmd',
	'vmd/ankha.vmd'
];
const loader = new MMDLoader();
let animation = false;

const clock = new THREE.Clock();

Ammo().then(function (AmmoLib) {

	Ammo = AmmoLib;

	init();

});

async function init() {
	click = 0;

	container = document.getElementById('container');

	//
	raycaster = new THREE.Raycaster();

	renderer = new THREE.WebGLRenderer({
		antialias: true
	});
	renderer.setPixelRatio(window.devicePixelRatio);
	renderer.setSize(window.innerWidth, window.innerHeight);
	renderer.setAnimationLoop(animate);
	renderer.toneMapping = THREE.ACESFilmicToneMapping;
	renderer.toneMappingExposure = 0.5;
	container.appendChild(renderer.domElement);

	//

	scene = new THREE.Scene();

	camera = new THREE.PerspectiveCamera(55, window.innerWidth / window.innerHeight, 1, 20000);
	// 初始化相机状态
    camera.position.set(-0.56, 22.57, 47.94);
    camera.rotation.set(-0.01, -0.01, -0.00);
    camera.zoom = 1;
    camera.fov = 55;

    camera.updateProjectionMatrix();

	

	sun = new THREE.Vector3();


	// Water

	const waterGeometry = new THREE.PlaneGeometry(10000, 10000);

	water = new Water(
		waterGeometry, {
		textureWidth: 512,
		textureHeight: 512,
		waterNormals: new THREE.TextureLoader().load('textures/waternormals.jpg', function (texture) {

			texture.wrapS = texture.wrapT = THREE.RepeatWrapping;

		}),
		sunDirection: new THREE.Vector3(),
		sunColor: 0xffffff,
		waterColor: 0x001e0f,
		distortionScale: 3.7,
		fog: scene.fog !== undefined
	}
	);

	water.rotation.x = -Math.PI / 2;

	scene.add(water);

	// Skybox

	const sky = new Sky();
	sky.scale.setScalar(10000);
	scene.add(sky);

	const skyUniforms = sky.material.uniforms;

	skyUniforms['turbidity'].value = 10;
	skyUniforms['rayleigh'].value = 2;
	skyUniforms['mieCoefficient'].value = 0.005;
	skyUniforms['mieDirectionalG'].value = 0.8;

	const parameters = {
		elevation: 2,
		azimuth: 180
	};

	// 圆台
	const geometry = new THREE.CylinderGeometry(18, 18, 6, 64, 1);

	// 加载法线纹理
	const normalTexture = new THREE.TextureLoader().load('textures/stillwater.jpg', function (texture) {
		texture.wrapS = texture.wrapT = THREE.RepeatWrapping;
	});
	const waterMaterial = new THREE.MeshStandardMaterial({
		map: normalTexture,
		transparent: true,
		opacity: 0.5, // 透明度
		blending: THREE.NormalBlending,
		side: THREE.FrontSide
	});

	mesh = new THREE.Mesh(geometry, waterMaterial);
	mesh.name = "geometry";
	group = new THREE.Group();
	group.add(mesh);
	scene.add(group);


	const pmremGenerator = new THREE.PMREMGenerator(renderer);
	const sceneEnv = new THREE.Scene();

	let renderTarget;

	function updateSun() {

		const phi = THREE.MathUtils.degToRad(90 - parameters.elevation);
		const theta = THREE.MathUtils.degToRad(parameters.azimuth);

		sun.setFromSphericalCoords(1, phi, theta);

		sky.material.uniforms['sunPosition'].value.copy(sun);
		water.material.uniforms['sunDirection'].value.copy(sun).normalize();

		if (renderTarget !== undefined) renderTarget.dispose();

		sceneEnv.add(sky);
		renderTarget = pmremGenerator.fromScene(sceneEnv);
		scene.add(sky);

		scene.environment = renderTarget.texture;

	}

	updateSun();


	//

	orbitControls = new OrbitControls(camera, renderer.domElement);
	orbitControls.target.set(0.00, 22.08, -7.66);
	orbitControls.minDistance = 20.0;
	orbitControls.maxDistance = 200.0;
	orbitControls.update();

	// 监听控制器变化
	orbitControls.addEventListener('change', () => {
		// 保存相机状态
		const cameraState = {
			position: {
				x: camera.position.x,
				y: camera.position.y,
				z: camera.position.z
			},
			rotation: {
				x: camera.rotation.x,
				y: camera.rotation.y,
				z: camera.rotation.z
			},
			zoom: camera.zoom,
			fov: camera.fov,
			target: {
				x: orbitControls.target.x,
				y: orbitControls.target.y,
				z: orbitControls.target.z
			}
		};

		
	});

	// 还原 OrbitControls target
	if (cameraState && cameraState.target) {
		orbitControls.target.set(
			cameraState.target.x,
			cameraState.target.y,
			cameraState.target.z
		);
		orbitControls.update();
	}


	




	const waterUniforms = water.material.uniforms;


	loadingManager.onProgress = function (url, itemsLoaded, itemsTotal) {
		console.log('Loading file: ' + url + '.\nLoaded ' + itemsLoaded + ' of ' + itemsTotal + ' files.');
	};

	loadingManager.onError = function (url) {
		console.log('There was an error loading ' + url);
	};

	// ------------模型-----------------------------------------



	let modelConfig;
	let modelSettings;
	console.log('getModelSetting', modelSettings);

	// 如果modelSettings为undefined，则设置默认模型
	if (modelSettings == undefined) {
		modelFile = defaultModel;
		modelConfig = {
			currentModel: 'wanhua'
		};
	} else {
		modelFile = modelSettings.path;
		console.log('modelSettings path:', modelSettings.path);
		modelConfig = {
			currentModel: modelSettings.name
		};
	}

	// ------------姿势-----------------------------------------

	const vpdFiles = [
		'vpd/01.vpd',
		'vpd/02.vpd',
		'vpd/03.vpd',
		'vpd/04.vpd',
		'vpd/05.vpd',
		'vpd/06.vpd',
		'vpd/07.vpd',
		'vpd/08.vpd',
		'vpd/09.vpd',
		'vpd/10.vpd',
		'vpd/11.vpd'
	];
	const controls = {};

	function initControls() {
		controls.姿势 = - 1;
		for (let i = 0; i < vpdFiles.length; i++) {
			controls[getBaseName(vpdFiles[i])] = false;
		}
	}



	initAnimationControls();

	// ------------动作-----------------------------------------

	const urlParams = new URLSearchParams(window.location.search);
	// 使用 modelFile 变量
	console.log('加载模型：', modelFile);
	const vmdFile = urlParams.get('motion') || 'vmd/idle.vmd';
	console.log('加载动作：', vmdFile);

	loadModel(modelFile, vmdFile);

	let vpdIndex = 0;

	function loadVpd() {
		const vpdFile = vpdFiles[vpdIndex];
		loader.loadVPD(vpdFile, false, function (vpd) {
			vpds.push(vpd);
			vpdIndex++;
			if (vpdIndex < vpdFiles.length) {
				loadVpd();
			} else {
				// console.log("vpd加载完成");
			}
		}, function (xhr) {
		}, function (error) {
		});
	}
	loadVpd();



	function onChangePose() {
		animationHelper.enable('animation', false);
		const index = parseInt(controls.姿势);
		if (currentModel === undefined) {
			return;
		}
		if (index === - 1) {
			currentModel.pose();
		} else {
			animationHelper.pose(currentModel, vpds[index]);
		}
	}

	// 添加环境光
	const ambientLight = new THREE.AmbientLight(0xffffff, 3);
	scene.add(ambientLight);


	mouse = new THREE.Vector2();
	renderer.domElement.addEventListener('click', onDocumentMouseDown, false);

	window.addEventListener('resize', onWindowResize);

	// onChangePose();
	pauseContinue();



	const chatGui = new GUI({
		title: '聊天面板',
		container: document.body, // 确保GUI添加到body
		autoPlace: true,         // 允许自动定位
		dragAndDrop: false,      // 禁用拖放
		closed: false            // 初始展开状态
	});
	// 设置位置和样式
	chatGui.domElement.style.position = 'absolute';
	chatGui.domElement.style.left = '0';
	chatGui.domElement.style.bottom = '0';
	chatGui.domElement.style.position = 'fixed';
	chatGui.domElement.style.width = '100%';  // 设置宽度为100%
	chatGui.domElement.style.maxWidth = '100%'; // 覆盖lil-gui的默认最大宽度
	// 创建左下角的聊天面板
	setupGUI(chatGui);
}

function loadModel(modelFile, vmdFile) {
	loader.loadWithAnimation(modelFile, vmdFile, function (mmd) {
		currentModel = mmd.mesh;
		window.currentModel = currentModel;
		// 设置模型的初始位置和大小 
		currentModel.scale.set(2, 2, 2);
		currentModel.position.set(0, 3, 0);
		currentModel.up.set(0, 1, 0);

		// 修改这部分，添加动画混合设置
		const mixer = new THREE.AnimationMixer(currentModel);
		const action = mixer.clipAction(mmd.animation);

		// 设置动画循环
		action.setLoop(THREE.LoopRepeat);
		const fadeTime = 0.5;
		const duration = action.getClip().duration;

		// 设置动画参数
		action.play();

		// 在动画更新时检查并处理过渡
		mixer.addEventListener('update', function (e) {
			// 获取当前动画时间
			const currentTime = action.time;

			// 在动画即将结束时开始过渡（比如最后0.5秒）
			if (currentTime >= duration - fadeTime && !action.userData.transitioning) {
				action.userData.transitioning = true;

				// 创建新的动作实例用于下一个循环
				const nextAction = mixer.clipAction(action.getClip());
				nextAction.setLoop(THREE.LoopRepeat);
				nextAction.time = 0; // 确保从头开始播放

				// 设置交叉淡入淡出
				nextAction.crossFadeFrom(action, fadeTime, true);
				nextAction.play();

				// 在过渡完成后重置标记
				setTimeout(() => {
					action.userData.transitioning = false;
				}, fadeTime * 1000);
			}
		});

		// 添加到 animationHelper
		animationHelper.add(currentModel, {
			animation: mmd.animation,
			physics: true,
			mixer: mixer,
			physicsElapsedTime: 1 / 60
		});
		group.add(currentModel);
		scene.add(group);

		// 初始化预加载所有动画
		initializeAnimations();
	},
		function (xhr) {
			// console.log((xhr.loaded / xhr.total * 100) + '% loaded');
		},
		function (error) {
			console.log('An error happened' + error);
		}
	);
}

function initializeAnimations() {
	let vmdIndex = 0;

	function loadVmd() {
		const vmdFile = vmdFiles[vmdIndex];

		loader.loadAnimation(vmdFile, currentModel, function (animation) {
			vmds.push(animation);
			vmdIndex++;
			if (vmdIndex < vmdFiles.length) {
				loadVmd();
			} else {
				console.log("所有动画预加载完成");
			}
		});
	}

	loadVmd();
}

function initAnimationControls() {
	animationControls.动作 = - 1;
	for (let i = 0; i < vmdFiles.length; i++) {
		animationControls[getBaseName(vmdFiles[i])] = false;
	}
}

function onChangeAnimation() {
	const index = parseInt(animationControls.动作);
	const vmdFile = vmdFiles[index];
	loadNewVmd(vmdFile);
}

// 播放/暂停
function pauseContinue() {
	if (animation) {
		console.log('暂停动画');
		animation = false;
	} else {
		console.log('播放动画');
		animation = true;
	}

	animationHelper.enable('animation', animation);
}


// 全局方法，切换新动画
window.loadNewVmd = function (vmdPath) {
	const helper = animationHelper.objects.get(currentModel);
	if (!helper) return;

	const mixer = helper.mixer;

	// 停止所有当前动画
	mixer.stopAllAction();

	// 从预加载的动画中查找
	const vmdIndex = vmdFiles.indexOf(vmdPath);
	if (vmdIndex === -1) {
		console.error('未找到预加载的动画:', vmdPath);
		return;
	}

	try {
		// 使用预加载的动画创建action
		const newAction = mixer.clipAction(vmds[vmdIndex]);

		// 设置动画参数
		newAction.setLoop(THREE.LoopRepeat);
		newAction.enabled = true;
		newAction.setEffectiveWeight(1);
		newAction.setEffectiveTimeScale(1);

		// 使用fadeIn过渡
		newAction.fadeIn(0.5);
		newAction.play();

		console.log('切换到预加载的动画:', vmdPath);

	} catch (e) {
		console.error('播放动画时出错:', e);
	}
}



function onWindowResize() {

	camera.aspect = window.innerWidth / window.innerHeight;
	camera.updateProjectionMatrix();

	renderer.setSize(window.innerWidth, window.innerHeight);

}

function animate() {

	render();
	//stats.update();

}



function render() {

	const time = performance.now() * 0.001;

	//mesh.position.y = Math.sin( time ) * 20 + 5;

	// 添加安全检查
	if (group && group.rotation) {
		if (click == 1) {
			group.rotation.y += -0.005;
		}
		// 移除多余的else分支，因为 group.rotation.y = group.rotation.y 没有实际作用
	}


	if (water && water.material && water.material.uniforms) {
		water.material.uniforms['time'].value += 1.0 / 60.0;
	}

	// 播放动画
	animationHelper.update(clock.getDelta());

	renderer.render(scene, camera);
}


function onDocumentMouseDown(event) {
	event.preventDefault();
	mouse.x = (event.clientX / window.innerWidth) * 2 - 1;
	mouse.y = -(event.clientY / window.innerHeight) * 2 + 1;

	raycaster.setFromCamera(mouse, camera);
	const intersects = raycaster.intersectObjects(scene.children);

	if (intersects.length > 0) {
		const object = intersects[0].object;

		if (object.name == 'geometry' && click == 0) {
			// 旋转
			click = 1;
		} else {
			click = 0;
		}
	}
}


function getBaseName(s) {
	return s.slice(s.lastIndexOf('/') + 1);
}

