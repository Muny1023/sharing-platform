# my-project-ai

资源分享社区的精简 AI Agent 服务。它通过 LangChain 调用 DeepSeek，提供社区帖子搜索和按需正文概括工具。

## 启动

1. 激活环境：`conda activate langchain1.2`
2. 安装依赖：`pip install -r requirements.txt`
3. 设置 `.env.example` 中列出的环境变量，Java 与 Python 的 `AI_SERVICE_TOKEN` 必须一致。
4. 启动服务：`uvicorn app.main:app --host 127.0.0.1 --port 8000`

也可以在 VS Code 中打开 `run_ai.ipynb`，选择 `Python (langchain1.2)` 内核，运行第一个代码单元启动服务；运行第二个代码单元停止由 Notebook 启动的服务。重复运行启动单元不会重复占用 8000 端口。

健康检查：`GET http://127.0.0.1:8000/health`

聊天请求可选传入 `targetPostId` 触发单篇正文概括；响应中的 `summary` 为模型生成的概括。正文只通过 Java 内部接口读取，已删除、空正文或超过 10000 字时返回明确提示。
