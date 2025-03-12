// 检查环境并设置日志重定向
let androidConsole;
try {
    // 尝试使用 nodejs-mobile 提供的 android 模块
    const android = process.binding('android');
    androidConsole = {
        log: function() {
            const msg = Array.prototype.map.call(arguments, item => {
                return (item === undefined) ? 'undefined' : item.toString();
            }).join(' ');
            android.log(msg);
        }
    };
    // 替换全局 console
    global.console = Object.assign(console, androidConsole);
} catch (e) {
    // 如果不在 Android 环境中，使用标准 console
    console.log('不在 Android 环境中，使用标准 console');
}

// 测试日志输出
console.log('Node.js启动成功！');
console.log('版本:', process.version);
console.log('工作目录:', process.cwd());

var express = require('express');
var path = require('path');
var fs = require('fs');
var cors = require('cors');
const { createProxyMiddleware } = require('http-proxy-middleware');

// 创建 Express 应用
var app = express();

// 启用 CORS
app.use(cors());

// 添加错误处理中间件
app.use((err, req, res, next) => {
    console.log('错误:', err.message);
    res.status(500).send('服务器错误: ' + err.message);
});

// 添加请求日志中间件
app.use((req, res, next) => {
    console.log(`收到请求: ${req.method} ${req.url}`);
    next();
});

// 设置静态文件目录
const publicPath = path.join(__dirname, 'public');
console.log('静态文件目录:', publicPath);
// 定义 indexPath 变量
const indexPath = path.join(publicPath, 'index.html');
console.log('首页文件路径:', indexPath);
app.use(express.static(publicPath));

// 路由处理
app.get('/', function(req, res) {
    console.log('访问根路径/');
    res.sendFile(indexPath);
});

app.get('/home', function(req, res) {
    console.log('访问/home路径');
    res.sendFile(indexPath);
});

// 记录所有请求的中间件
app.use((req, res, next) => {
    console.log('Global middleware:', req.method, req.path);
    next();
});

// 专门记录 API 请求的中间件
app.use('/proxy/api', (req, res, next) => {
    next();
}, createProxyMiddleware({
    router: (req) => {
        // 从请求头中获取目标URL
        return req.headers['x-target-url'];
    },
    changeOrigin: true,
    pathRewrite: {
        '^/proxy/api': ''
    },
    logLevel: 'debug',
    onProxyReq: (proxyReq, req, res) => {
        try {
            if (req.method === 'POST' && req.body) {
                const bodyData = JSON.stringify(req.body);
                //logToFile(`Request Body: ${bodyData}`);
                proxyReq.setHeader('Content-Length', Buffer.byteLength(bodyData));
                proxyReq.write(bodyData);
            }
        } catch (error) {
            //logToFile(`Error in onProxyReq: ${error.message}`);
        }
    },
    onProxyRes: (proxyRes, req, res) => {
        
        // 收集响应数据
        let responseBody = '';
        proxyRes.on('data', function(chunk) {
            responseBody += chunk;
        });
        proxyRes.on('end', function() {
            //logToFile(`Response Body: ${responseBody}`);
        });
    },
    onError: (err, req, res) => {
        res.status(500).json({
            error: err.message,
            code: err.code,
            stack: err.stack
        });
    }
}));

app.use('/proxy/ttss', (req, res, next) => {
    next();
}, createProxyMiddleware({
    router: (req) => {
        // 从请求头中获取目标URL
        return req.headers['x-target-url'];
    },
    changeOrigin: true,
    pathRewrite: {
        '^/proxy/ttss': ''
    },
    logLevel: 'debug',
    onProxyReq: (proxyReq, req, res) => {
        try {
            if (req.method === 'POST' && req.body) {
                const bodyData = JSON.stringify(req.body);
                proxyReq.setHeader('Content-Length', Buffer.byteLength(bodyData));
                proxyReq.write(bodyData);
            }
            
        } catch (error) {
            //logToFile(`Error in onProxyReq: ${error.message}`);
        }
    },
    onProxyRes: (proxyRes, req, res) => {
        // 收集响应数据
        let responseBody = '';
        proxyRes.on('data', function(chunk) {
            responseBody += chunk;
        });
        proxyRes.on('end', function() {
        });
    },
    onError: (err, req, res) => {
        res.status(500).json({
            error: err.message,
            code: err.code,
            stack: err.stack
        });
    }
}));


// 启动服务器
const port = 3000;
app.listen(port, '0.0.0.0', () => {
    console.log(`服务器启动在端口 ${port}`);
});

// 在现有的Express应用中添加以下代码
// 添加解析JSON的中间件
app.use(express.json());

// 添加接收日志的路由
app.post('/log', function(req, res) {
    // 使用重定向后的console.log输出到Logcat
    console.log('[Browser]', req.body.message);
    res.sendStatus(200);
});

// 其余代码保持不变