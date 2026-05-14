# 灵犀 Prompt

AI 提示词库管理器，基于 Java Spring Boot + PWA。项目用于 Java 课程期末作业展示，支持本地数据库存储、提示词管理、分类标签、搜索筛选和一键复制。

## 功能

- 提示词新增、编辑、删除、列表展示
- 分类新增、改名、删除
- 标签自动汇总与标签筛选
- 关键词、分类、标签组合筛选
- 一键复制提示词内容
- PWA 安装与基础离线缓存
- JSON 导入 / 导出后端接口

## 技术栈

- Java 21
- Spring Boot 4
- Spring Data JPA
- H2 本地数据库
- 原生 HTML / CSS / JavaScript PWA

## 本地运行

```powershell
.\mvnw.cmd spring-boot:run
```

启动后访问：

```text
http://localhost:8080
```

H2 数据库文件会自动生成在 `data/` 目录，该目录是本地运行产物，不需要提交到 GitHub。

## 常用接口

- `GET /api/prompts`
- `POST /api/prompts`
- `PUT /api/prompts/{id}`
- `DELETE /api/prompts/{id}`
- `GET /api/categories`
- `GET /api/tags`
- `POST /api/import`
- `GET /api/export`

## 部署说明

本项目后端是 Spring Boot 长驻服务，并使用 H2 本地文件数据库。适合本地运行或部署到支持 Java 服务的平台，例如 Render、Railway、Fly.io、Koyeb 等。

如果只部署到 Vercel，需要改造成纯静态前端或拆分为前后端部署。

### Render 部署

仓库已包含 `Dockerfile` 和 `render.yaml`，可以在 Render 中直接连接 GitHub 仓库创建 Web Service。

Render 会提供 `PORT` 环境变量，应用会通过 `server.port=${PORT:8080}` 自动绑定端口。

注意：默认 H2 数据库写在容器内的 `data/` 目录，免费 Web Service 重启或重新部署后数据可能会丢失。课程演示可以接受；如果要长期保存，需要在 Render 配置持久磁盘或改用外部数据库。
