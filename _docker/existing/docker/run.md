
#  Docker Compose 安装中间件和应用

## 安装基础中间件 (MySQL、Redis、Minio、Xxl-Job)

```bash
docker-compose -f ./docker-compose.yml -p youlai-boot up -d
```

## 安装完整环境 (包含中间件、后端应用和Nginx)

### 构建前端

```bash
cd ../../frontend
npm install
npm run build
```

### 准备前端应用目录

在启动 Docker 容器之前，需要创建前端应用目录并将构建好的 Vue 应用复制到相应的目录中：

```bash
# 返回到 docker 目录
cd ../backend/docker

# 创建前端应用目录
mkdir -p ./nginx/html/retail
mkdir -p ./nginx/html/booking
mkdir -p ./nginx/html/gallery
mkdir -p ./nginx/html/weather
mkdir -p ./nginx/html/meal

# 复制零售应用
cp -r ../../frontend/dist/* ./nginx/html/retail/

# 复制其他 Vue 应用（如果有）
# 例如：
# cp -r /path/to/BookingHotel/dist/* ./nginx/html/booking/
# cp -r /path/to/PhotoGallary/dist/* ./nginx/html/gallery/
# cp -r /path/to/Weather/dist/* ./nginx/html/weather/
# cp -r /path/to/mealplanner/dist/* ./nginx/html/meal/
```

### 启动 Docker 容器

```bash
docker-compose -f ./docker-compose-env.yml -p youlai-retail up -d
```

- p youlai-boot/youlai-retail 指定命名空间，避免与其他容器冲突，这里方便管理，统一管理和卸载

## 卸载基础中间件
```bash
docker-compose -f ./docker-compose.yml -p youlai-boot down
```

## 卸载完整环境
```bash
docker-compose -f ./docker-compose-env.yml -p youlai-retail down
```

## 访问
- 后端API: http://localhost:8989
- 应用门户: http://localhost
- 各个前端应用:
  - 零售应用: http://localhost/retail/
  - 酒店预订应用: http://localhost/booking/
  - 照片库应用: http://localhost/gallery/
  - 天气应用: http://localhost/weather/
  - 膳食计划应用: http://localhost/meal/
- Nginx代理后的API: http://localhost/api/

## CI/CD 自動デプロイ

このプロジェクトは GitHub Actions を使用して、フロントエンドまたはバックエンドのソースコードが変更された場合に、自動的に Konoha VPS にデプロイされるように設定されています。詳細については、[cicd.md](env-cicd.md) を参照してください。
