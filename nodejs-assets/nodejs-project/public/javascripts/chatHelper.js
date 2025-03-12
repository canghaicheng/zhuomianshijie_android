import { SentencePreloadQueue, speakContentWithTtsSetting } from './newAudioHelper.js';

// const apiSettingObject = {
//     apiMode: "openai",
//     apiDomain: "https://api.siliconflow.cn",
//     apiPath: "/v1/chat/completions",
//     model: "Qwen/Qwen2.5-7B-Instruct",
//     maxSize: 32
// };

//const ttsSettingObject = {
//    ttsDomain: "https://api.minimax.chat",
//    ttsPath: "/v1/t2a_v2?GroupId=1862063111012356333",
//    ttsModel: "speech-01-turbo",
//    ttsVoiceId: "female-yujie-jingpin"
//}

// const sttSettingObject = {
//     sttDomain : "https://api.siliconflow.cn",
//     sttPath : "/v1/audio/transcriptions",
//     sttKey : "",
//     sttModel : "FunAudioLLM/SenseVoiceSmall"
// }

let apiSettingObject ;
let ttsSettingObject;
let sttSettingObject;

// 初始化时加载API设置
async function loadApiSetting() {
    try {
        console.log('开始加载API设置...');
        // 从Android本地HTTP服务器获取API设置
        const response = await fetch('http://localhost:8080/api/settings', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const currentSetting = await response.json();
        console.log('成功获取API设置:', currentSetting);
        
        if (currentSetting) {
            apiSettingObject = {
                apiMode: currentSetting.apiMode || "openai",
                apiDomain: currentSetting.apiDomain || "",
                apiPath: currentSetting.apiPath || "",
                apiKey: currentSetting.apiKey || "",
                model: currentSetting.model || "",
                maxSize: currentSetting.maxSize || 32
            };
            // 详细打印每个设置值
            console.log('API模式:', apiSettingObject.apiMode);
            console.log('API域名:', apiSettingObject.apiDomain);
            console.log('API路径:', apiSettingObject.apiPath);
            console.log('API密钥:', apiSettingObject.apiKey);
            console.log('模型名称:', apiSettingObject.model);
            console.log('上下文大小:', apiSettingObject.maxSize);
        }
    } catch (error) {
        console.error('加载API设置失败:', error);
        // 设置默认值，确保应用可以继续运行
        apiSettingObject = {
            apiMode: "openai",
            apiDomain: "",
            apiPath: "",
            apiKey: "",
            model: "",
            maxSize: 32
        };
        console.log('使用默认API设置:', apiSettingObject);
    }
}

// 初始化时加载TTS设置
async function loadTtsSetting() {
    try {
        console.log('开始加载TTS设置...');
        // 从Android本地HTTP服务器获取TTS设置
        const response = await fetch('http://localhost:8081/tts/settings', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const currentSetting = await response.json();
        console.log('成功获取TTS设置:', currentSetting);
        
        if (currentSetting) {
            ttsSettingObject = {
                ttsDomain: currentSetting.ttsDomain || "",
                ttsPath: currentSetting.ttsPath || "",
                ttsKey: currentSetting.ttsKey || "",
                ttsModel: currentSetting.ttsModel || "",
                ttsVoiceId: currentSetting.ttsVoiceId || ""
            };
            // 详细打印每个设置值
            console.log('TTS域名:', ttsSettingObject.ttsDomain);
            console.log('TTS路径:', ttsSettingObject.ttsPath);
            console.log('TTS密钥:', ttsSettingObject.ttsKey);
            console.log('TTS模型:', ttsSettingObject.ttsModel);
            console.log('音色编号:', ttsSettingObject.ttsVoiceId);
        }
    } catch (error) {
        console.error('加载TTS设置失败:', error);
        // 设置默认值，确保应用可以继续运行
        ttsSettingObject = {
            ttsDomain: "",
            ttsPath: "",
            ttsKey: "",
            ttsModel: "",
            ttsVoiceId: ""
        };
        console.log('使用默认TTS设置:', ttsSettingObject);
    }
}

// 初始化时加载STT设置
async function loadSttSetting() {
    try {
        console.log('开始加载STT设置...');
        // 从Android本地HTTP服务器获取STT设置
        const response = await fetch('http://localhost:8082/stt/settings', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const currentSetting = await response.json();
        console.log('成功获取STT设置:', currentSetting);
        
        if (currentSetting) {
            sttSettingObject = {
                sttDomain: currentSetting.sttDomain || "",
                sttPath: currentSetting.sttPath || "",
                sttKey: currentSetting.sttKey || "",
                sttModel: currentSetting.sttModel || ""
            };
            // 详细打印每个设置值
            console.log('STT域名:', sttSettingObject.sttDomain);
            console.log('STT路径:', sttSettingObject.sttPath);
            console.log('STT密钥:', sttSettingObject.sttKey);
            console.log('STT模型:', sttSettingObject.sttModel);
            return sttSettingObject;
        }
    } catch (error) {
        console.error('加载STT设置失败:', error);
        // 设置默认值，确保应用可以继续运行
        sttSettingObject = {
        };
        console.log('STT未设置');
        return sttSettingObject;
    }
}




// 修改 setupGUI 函数，添加加载 TTS 设置
export async function setupGUI(chatGui) {
    // 创建聊天框文件夹
    const messageFolder = chatGui;
    setupMessageFolder(messageFolder);

    // 确保API设置和TTS设置已加载
    await Promise.all([loadApiSetting(), loadTtsSetting()]);
}


// 定义一个messages为jsonArray
let messages = [];



function setupMessageFolder(messageFolder) {
    const folderContainer = document.createElement('div');
    folderContainer.style.padding = '0 6px';
    folderContainer.style.width = '100%';
    // 命名
    folderContainer.id = 'message-folder';


    const sendButtonContainer = createSendButton();
    // 分开获取container 和 sendButton
    const container = sendButtonContainer.container;
    const recordButton = sendButtonContainer.recordButton;

    // 独立处理语音识别初始化
    initSpeechRecognition(recordButton).catch(error => {
        console.warn('语音识别初始化失败，禁用录音功能:', error);
        recordButton.style.display = 'none';
    });


    folderContainer.appendChild(container);

    hideEmptyText(messageFolder);
    messageFolder.domElement.appendChild(folderContainer);
    messageFolder.open();

    setupFolderToggle(messageFolder, folderContainer);
}

// 录音相关变量
let mediaRecorder = null;  // MediaRecorder实例
let audioChunks = [];     // 用于存储录音数据

// 将语音识别初始化独立为一个函数
async function initSpeechRecognition(recordButton) {
    try {
        // 获取stt设置
        const sttSetting = await loadSttSetting();
        // 如果为空
        if (!sttSetting || Object.keys(sttSetting).length === 0) {
            throw new Error('stt未配置');
        }
        
        // 检查浏览器是否支持 mediaDevices API
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            throw new Error('当前环境不支持录音功能，请确保在支持的浏览器中运行');
        }
        
        // 最新的stt设置
        const sttApiDomain = sttSetting.sttDomain;
        const sttApiKey = sttSetting.sttKey;
        const sttApiPath = sttSetting.sttPath;
        const sttModel = sttSetting.sttModel;

        // recordButton的click事件绑定CapsLock键
        // 记录上一次的CapsLock状态
        let lastCapsLockState = false;

        // 监听keydown事件
        document.addEventListener('keydown', (e) => {
            // 使用setTimeout确保获取到正确的CapsLock状态
            setTimeout(() => {
                const capsLockState = e.getModifierState('CapsLock');
                // 只在状态发生变化时触发
                if (capsLockState !== lastCapsLockState) {
                    recordButton.click();
                    lastCapsLockState = capsLockState;
                }
            }, 0);
        });

        let isRecording = false;
        
        console.log('请求麦克风权限...');
        // 请求麦克风权限并获取音频流
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        console.log('麦克风权限获取成功');

        // 创建 MediaRecorder 实例
        mediaRecorder = new MediaRecorder(stream);
        
        // 监听录音数据
        mediaRecorder.ondataavailable = (event) => {
            audioChunks.push(event.data);
        };

        // 监听录音结束
        mediaRecorder.onstop = async () => {
            // 将录音数据转换为wav文件
            const audioBlob = new Blob(audioChunks, { type: 'audio/wav' });
            const audioFile = new File([audioBlob], 'recording.wav', { type: 'audio/wav' });

            // 准备FormData
            const form = new FormData();
            form.append('file', audioFile);
            form.append('model', sttModel);

            try {
                // 调用语音识别接口
                const response = await fetch(sttApiDomain + sttApiPath, {
                    method: 'POST',
                    headers: {
                        'Authorization': 'Bearer ' + sttApiKey
                    },
                    body: form
                });

                const result = await response.json();
                console.log('语音识别结果:', result.text);
                if (result.text) {
                    sendMessage(result.text);
                }

                // 清空录音数据
                audioChunks = [];

            } catch (error) {
                console.error('语音识别失败:', error);
            }
        };

        // 设置录音按钮点击事件
        recordButton.addEventListener('click', () => {
            if (!isRecording) {
                // 开始录音
                console.log('开始录音');
                audioChunks = []; // 清空之前的录音数据
                mediaRecorder.start(); // MediaRecorder的start方法
                isRecording = true;
                updateButtonState(true);
            } else {
                // 停止录音
                console.log('停止录音');
                mediaRecorder.stop(); // MediaRecorder的stop方法
                isRecording = false;
                updateButtonState(false);
            }
        });

        return true;

    } catch (error) {
        console.error('录音初始化失败:', error);
        return false;
    }

    function updateButtonState(recording) {
        const micIcon = recordButton.querySelector('img');
        if (recording) {
            micIcon.src = 'pictures/voice-off.png';
            recordButton.title = '点击停止录音';
        } else {
            micIcon.src = 'pictures/voice.png';
            recordButton.title = '点击开始录音';
        }
    }
}





function setupInputArea(textarea, className, placeholder) {
    textarea.className = className;
    textarea.placeholder = placeholder;
    textarea.style.width = '100%';
    textarea.style.minHeight = '25px';
    textarea.style.margin = '6px 0';
    textarea.style.padding = '4px';
    textarea.style.boxSizing = 'border-box';
    textarea.style.backgroundColor = '#1a1a1a';
    textarea.style.color = '#fff';
    textarea.style.border = '1px solid #333';
    textarea.style.borderRadius = '4px';
    textarea.style.resize = 'vertical';
    textarea.style.fontSize = '12px';

    // 添加焦点样式
    textarea.addEventListener('focus', () => {
        textarea.style.outline = 'none';
        textarea.style.border = '1px solid #ffffff';
    });

    // 添加失去焦点时的样式
    textarea.addEventListener('blur', () => {
        textarea.style.border = '1px solid #333';
    });
}

function setupTextAreaUp(textarea, className, placeholder) {
    textarea.className = className;
    textarea.placeholder = placeholder;
    textarea.style.width = '100%';
    textarea.style.minHeight = '32px';
    textarea.style.lineHeight = '32px';  //
    textarea.style.height = '32px';     // 添加这行,设置初始高度
    textarea.style.margin = '4px 0';
    textarea.style.boxSizing = 'border-box';
    textarea.style.backgroundColor = '#1a1a1a';
    textarea.style.color = '#fff';
    textarea.style.border = '1px solid #333';
    textarea.style.borderRadius = '4px';
    textarea.style.resize = 'none';      // 禁用原生resize
    textarea.style.fontSize = '12px';


    // 创建自定义resize手柄
    const resizer = document.createElement('div');
    resizer.style.position = 'absolute';
    resizer.style.right = '4px';         // 调整到输入框内部右上角
    resizer.style.top = '10px';          // 调整到输入框内部上
    resizer.style.width = '6px';         // 更小的手柄
    resizer.style.height = '6px';        // 更小的手柄
    resizer.style.cursor = 'n-resize';   // 上下拖动的光标
    resizer.style.borderRight = '2px solid #666';  // 使用边框创建手柄样式
    resizer.style.borderTop = '2px solid #666';
    resizer.style.zIndex = '10';         // 确保手柄在最上层
    resizer.style.pointerEvents = 'all'; // 确保可以点击

    // 添加拖动逻辑
    let startY, startHeight;
    resizer.addEventListener('mousedown', (e) => {
        startY = e.clientY;
        startHeight = parseInt(getComputedStyle(textarea).height);
        document.addEventListener('mousemove', resize);
        document.addEventListener('mouseup', stopResize);
        // e.preventDefault(); // 防止文本选择
    });

    function resize(e) {
        const deltaY = startY - e.clientY;
        textarea.style.height = `${startHeight + deltaY}px`;
    }

    function stopResize() {
        document.removeEventListener('mousemove', resize);
        document.removeEventListener('mouseup', stopResize);
    }

    // 将resize手柄添加到textarea的容器中
    const container = document.createElement('div');
    container.style.position = 'relative';
    container.style.width = '100%';      // 保容器宽度正确

    container.appendChild(textarea);
    container.appendChild(resizer);

    // 添加焦点样式
    textarea.addEventListener('focus', () => {
        textarea.style.outline = 'none';
        textarea.style.border = '1px solid #ffffff';
    });

    // 添加失去焦点时的样式
    textarea.addEventListener('blur', () => {
        textarea.style.border = '1px solid #333';
    });

    return container;
}

function setupTextArea(textarea, className, placeholder) {
    textarea.className = className;
    textarea.placeholder = placeholder;
    textarea.style.width = '100%';
    textarea.style.minHeight = '50px';
    textarea.style.margin = '6px 0';
    textarea.style.padding = '4px';
    textarea.style.boxSizing = 'border-box';
    textarea.style.backgroundColor = '#1a1a1a';
    textarea.style.color = '#fff';
    textarea.style.border = '1px solid #333';
    textarea.style.borderRadius = '4px';
    textarea.style.resize = 'vertical';
    textarea.style.fontSize = '12px';

    // 添加焦点样式
    textarea.addEventListener('focus', () => {
        textarea.style.outline = 'none';
        textarea.style.border = '1px solid #ffffff';
    });

    // 添加失去焦点时的样式
    textarea.addEventListener('blur', () => {
        textarea.style.border = '1px solid #333';
    });
}

function createSendButton() {
    const urlParams = new URLSearchParams(window.location.search);
    const windowType = urlParams.get('window');
    console.log('windowType:', windowType);

    // 创建容器来包含点阵、录音按钮和发送按钮
    const container = document.createElement('div');
    container.style.display = 'flex';
    container.style.alignItems = 'center';
    container.style.gap = '8px';

    // 创建录音按钮
    const recordButton = document.createElement('button');
    recordButton.style.background = 'none';
    recordButton.style.border = 'none';
    recordButton.style.cursor = 'pointer';
    recordButton.style.padding = '0';
    recordButton.style.width = '40px'; // 调整宽度
    recordButton.title = '点击开始录音'; // 添加鼠标悬浮提示

    const micIcon = document.createElement('img');
    micIcon.src = 'pictures/voice.png';
    micIcon.alt = '录音';
    micIcon.style.width = '24px';
    micIcon.style.height = '24px';

    recordButton.appendChild(micIcon);

    const textarea = document.createElement('textarea');
    const textareaContainer = setupTextAreaUp(textarea, 'message-input', '');


    // 创建发送按钮
    const sendButton = document.createElement('button');
    sendButton.className = 'send-button';
    sendButton.textContent = '发送';
    sendButton.disabled = true;

    Object.assign(sendButton.style, {
        padding: '6px 12px',
        backgroundColor: '#363636',
        color: '#fff',
        border: 'none',
        borderRadius: '4px',
        cursor: 'pointer',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        height: '24px',
        lineHeight: '1',
        fontSize: '12px',
        opacity: '0.5',
        minWidth: '48px',
        flex: '1'  // 发送按钮占据剩余空间
    });

    if (windowType === 'transparent') {
        // 创建点nprogram器
        const dragHandle = document.createElement('div');
        dragHandle.style.display = 'grid';
        dragHandle.style.gridTemplateColumns = 'repeat(3, 2px)';
        dragHandle.style.gap = '2px';
        dragHandle.style.padding = '4px';
        dragHandle.style.cursor = 'move';
        dragHandle.style.webkitAppRegion = 'drag';  // 使其可拖动
        dragHandle.title = '点击拖动窗口'; // 添加鼠标悬浮提示

        for (let i = 0; i < 9; i++) {
            const dot = document.createElement('div');
            dot.style.width = '2px';
            dot.style.height = '2px';
            dot.style.backgroundColor = '#666';
            dot.style.borderRadius = '50%';
            dragHandle.appendChild(dot);
        }

        container.appendChild(dragHandle);
    }

    container.appendChild(recordButton);
    container.appendChild(textareaContainer);
    container.appendChild(sendButton);

    setupMessageHandling(textarea, sendButton);

    return {
        container,
        recordButton,
        sendButton
    };
}

// 创建预加载队列实例
const queue = new SentencePreloadQueue(3);

// 开启自动播放
queue.setAutoPlay(true);

async function sendMessage(message) {
    try {

        // 定义一个usercontent为jsonObject
        let userContent = {
            role: "user",
            content: message
        };

        let characterSetting;
        // 定义一个systemContent为jsonObject
        let systemContent = {
            role: "system",
            content: characterSetting
        };
        // messages中有且只有一个systemContent
        // 检查并处理 messages 数组
        if (messages.length === 0) {
            // 如果是空数组，添加 systemContent
            messages.push(systemContent);
        } else if (messages[0].role !== "system") {
            // 如果第一条不是 system 消息，在开头插入
            messages.unshift(systemContent);
        } else {
            // 如果第一条是 system 消息，更新它的内容
            messages[0] = systemContent;
        }

        // 将usercontent添加到messages中
        trimMessages(userContent);
        console.log(userContent);

       

        // 获取域名api-domain
        const target = apiSettingObject.apiDomain;
        console.log('target:', target);
        // 获取api-path
        const path = apiSettingObject.apiPath;
        console.log('path:', path);
        // 获取api-key
        const token = apiSettingObject.apiKey;
        console.log('token:', token);
        // 获取model-name
        const model = apiSettingObject.model;
        console.log('model:', model);

        const options = {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json',
                'X-Target-URL': target
            },
            body: JSON.stringify({
                model: model,
                messages: messages,
                stream: true,
                max_tokens: 4096,
                temperature: 0.7,
                top_p: 0.7,
                top_k: 50,
                frequency_penalty: 0.5,
                n: 1
            })
        };

        // 简单的异步生成器函数来读取流数据
        async function* readStream(reader) {
            let buffer = '';  // 用于存储未完成的数据
            const decoder = new TextDecoder();

            while (true) {
                const { value, done } = await reader.read();
                if (done) break;

                buffer += decoder.decode(value, { stream: true });

                // 按行分割，处理每个完整的data块
                const lines = buffer.split('\n');
                buffer = lines.pop() || ''; // 保留最后一个不完整的行

                for (const line of lines) {
                    const trimmedLine = line.trim();
                    if (trimmedLine.startsWith('data: ')) {
                        const jsonStr = trimmedLine.slice(5); // 移除 "data: "
                        if (jsonStr === '[DONE]') continue;

                        try {
                            const json = JSON.parse(jsonStr);
                            yield json;
                        } catch (e) {
                            console.warn('跳过不完整的JSON:', jsonStr);
                            continue;
                        }
                    }
                }
            }

            // 处理最后可能剩余的数据
            if (buffer.trim()) {
                const trimmedLine = buffer.trim();
                if (trimmedLine.startsWith('data: ')) {
                    const jsonStr = trimmedLine.slice(5);
                    if (jsonStr !== '[DONE]') {
                        try {
                            const json = JSON.parse(jsonStr);
                            yield json;
                        } catch (e) {
                            console.warn('跳过最后不完整的JSON:', jsonStr);
                        }
                    }
                }
            }
        }

        const response = await fetch('/proxy/api' + path, options);
        const reader = response.body.getReader();
        let content = '';
        let buffer = '';

        // 使用修改后的readStream处理流数据
        for await (const chunk of readStream(reader)) {
            const chunkContent = chunk.choices?.[0]?.delta?.content;
            if (chunkContent) {
                // console.log('chunkContent:', chunkContent);
                content += chunkContent;
                buffer += chunkContent;

                // 接口有速率限制，逻辑也有问题，先预留流式播放功能。。。
                // 累积chunkContent,如果遇到句号。或者.就开始播放之前的句子
                // if (chunkContent.includes('.') || chunkContent.includes('。') ) {
                //     // 去除content里的*
                //     const replaceContent = buffer.replace(/\*/g, '');
                //     if (ttsSetting === 'edgetts') {
                //         queue.addSentence(replaceContent);
                //     } else {

                //         // 获取ttsSetting对应的隐藏输入框值放入一个对象
                //         const ttsSettingObject = {
                //             ttsDomain: document.querySelector('.tts-domain').value.trim(),
                //             ttsPath: document.querySelector('.tts-path').value.trim(),
                //             ttsKey: document.querySelector('.tts-key').value.trim(),
                //             ttsModel: document.querySelector('.tts-model').value.trim(),
                //             ttsVoiceId: document.querySelector('.tts-voice-id').value.trim()
                //         };
                //         queue.addSentence(replaceContent, ttsSettingObject);
                //     }

                //     console.log('buffer:', buffer);
                //     buffer = '';
                // }
            }
        }

        // 定义一个assistantContent为jsonObject
        let assistantContent = {
            role: "assistant",
            content: content
        };
        trimMessages(assistantContent);
        console.log("content：" + content);
        // 不是undefined    

        // console.log('ttsSetting:', ttsSetting);
        if (content) {
            // 去除content里的*
            const replaceContent = content.replace(/\*/g, '');
            // 去除content里以<think>开头以</think>结尾的内容
            const replaceThinkContent = replaceContent.replace(/<think>[\s\S]*?<\/think>/g, '');

            // console.log("替换后的内容:", replaceThinkContent);
            speakContentWithTtsSetting(replaceThinkContent, ttsSettingObject);
        }

        return response.data;
    } catch (error) {
        console.error('发送消息时出错:', error);
        alert('发送消息失败: ' + error.message);
    }
}

// 添加消息前检查裁剪消息史
function trimMessages(newMessage) {
    // 计算当前消息的长度（包括新消息）
    const getMessageSize = msg => JSON.stringify(msg).length;
    // 获取max-size
    const maxSize = apiSettingObject.maxSize;
    const MAX_SIZE = maxSize * 1024; // 31K

    // 计算新消息的大小
    const newMessageSize = getMessageSize(newMessage);

    // 当前所有消息的总大小
    let totalSize = messages.reduce((size, msg) => size + getMessageSize(msg), 0) + newMessageSize;

    // 如果超过限制，从第二条消息开始删除（保留system消息）
    while (totalSize > MAX_SIZE && messages.length > 1) {
        // 每次删除一对对话（user和assistant消息）
        const removed1 = messages.splice(1, 1)[0]; // 删除用户消息
        const removed2 = messages.splice(1, 1)[0]; // 删除助手消息

        // 重新计算大小
        totalSize -= (getMessageSize(removed1) + getMessageSize(removed2));
    }

    // 添加新消息
    messages.push(newMessage);
}




function setupMessageHandling(textarea, sendButton) {
    let message = '';

    textarea.addEventListener('input', () => {
        message = textarea.value;
        sendButton.disabled = !message.trim();
        sendButton.style.opacity = sendButton.disabled ? '0.5' : '1';
        sendButton.style.cursor = sendButton.disabled ? 'not-allowed' : 'pointer';
    });

    sendButton.addEventListener('click', async () => {
        if (message.trim()) {
            // 判断有没有API设置
            let selectedApiSetting;
            // if (!selectedApiSetting) {
            //     alert('请配置API');
            //     return;
            // }
            // 清空输入框
            textarea.value = '';
            await sendMessage(message);

            message = '';
            sendButton.disabled = true;
            sendButton.style.opacity = '0.5';
            sendButton.style.cursor = 'not-allowed';
        }
    });

    textarea.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            if (!sendButton.disabled) {
                sendButton.click();
            }
        }
    });
}

function hideEmptyText(folder) {
    const emptyText = folder.domElement.querySelector('.children');
    if (emptyText) {
        emptyText.style.display = 'none';
    }
}

function setupFolderToggle(folder, container) {
    let isOpen = true;
    folder.$title.addEventListener('click', () => {
        if (isOpen) {
            folder.close();
            container.style.display = 'none';
        } else {
            folder.open();
            container.style.display = 'block';
        }
        isOpen = !isOpen;
    });
}

