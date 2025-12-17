## MACA - Android 前端 + .NET 8 后端（本地运行与部署指南）

本仓库包含：
- Android 前端（Kotlin、Retrofit）位于 `AndroidApp/`
- .NET 8 Web API 后端（EF Core + MySQL/Pomelo，可在开发模式下回退到内存数据库）位于 `Backend/MemoryGameBackend/`

本文档将指导你从零开始在本机复现运行，包括准备数据库、配置后端、配置前端（Android 模拟器/真机）、常见问题与排错。

---

### 1. 环境要求
- Windows 10/11（其它系统亦可，但静态广告目录路径需相应调整）
- .NET SDK 8.0+
- MySQL Server 8.0+（若使用内存数据库可跳过安装 MySQL，见下文“快速启动（内存数据库）”）
- Android Studio（含 Android SDK 与模拟器）

可选工具：
- MySQL Workbench（图形化管理数据库）
- Postman/浏览器用于调试后端 Swagger

---

### 2. 后端配置与运行
后端工程路径：`Backend/MemoryGameBackend/`

后端关键点：
- 监听地址：`http://0.0.0.0:5180`（开发）
- Swagger（开发环境）：`http://localhost:5180/swagger`
- 静态广告图片目录：默认映射 Windows 路径 `E:\ads` 到 URL 前缀 `/ad-images`
- 数据库：MySQL（通过 Pomelo.EntityFrameworkCore.MySql），开发模式下可回退到内存数据库（基于连接串判定）
- 认证：JWT（请设置一段足够随机的密钥）

#### 2.1 配置 `appsettings.json`
文件位置：`Backend/MemoryGameBackend/appsettings.json`

默认内容示例（请根据自身环境修改）：

```json
{
  "Logging": {
    "LogLevel": {
      "Default": "Information",
      "Microsoft.AspNetCore": "Warning"
    }
  },
  "AllowedHosts": "*",
  "ConnectionStrings": {
    "DefaultConnection": "Server=localhost;Database=maca;User=root;Password=YOUR_PASSWORD;"
  },
  "Jwt": {
    "Key": "REPLACE_WITH_A_LONG_RANDOM_SECRET",
    "Issuer": "MemoryGameApi",
    "Audience": "MemoryGameClient",
    "ExpireMinutes": 60
  }
}
```

重要说明：
- 请将 `YOUR_PASSWORD` 替换为你的 MySQL 密码。
- 强烈建议将敏感信息放入 `appsettings.Development.json`（已被 Git 忽略时更安全）或使用环境变量注入，避免泄露。
- JWT Key 必须是一段足够随机且较长的字符串。

#### 2.2 准备静态广告目录
- 在 Windows 上创建目录：`E:\ads`
- 放入若干图片文件（如 `ad1.jpg`, `ad2.png` …）
- 后端会将该目录映射为 `http://<后端主机>:5180/ad-images/<文件名>`
- 如无法使用 `E:\ads`，请在 `Program.cs` 中将 `PhysicalFileProvider(@"E:\ads")` 改为你的实际路径后再运行。

#### 2.3 快速启动（使用内存数据库）
后端在“开发环境”且连接串为空或包含占位标记时，会启用内存数据库并自动种子两个用户：
- 用户名/密码：`admin/admin`（Paid User）
- 用户名/密码：`guest/guest`（Free User）

使用步骤：
1) 将 `ConnectionStrings:DefaultConnection` 留空或改成包含占位（例如包含 `YOUR_PASSWORD_HERE`）；
2) 保持 `ASPNETCORE_ENVIRONMENT=Development`（默认 launchSettings 已设置）；
3) 运行后端（见 2.5）。

注意：内存库数据进程退出即丢失，仅用于快速试跑。

#### 2.4 使用 MySQL（生产/共享环境）
如果你希望让团队成员共享同一数据库或使用持久化数据，请按下文“数据库准备（MySQL）”创建数据库与表结构，并在 `appsettings.json` 设置正确连接串。

#### 2.5 运行后端
- PowerShell/命令行：

```powershell
cd Backend/MemoryGameBackend
dotnet restore
dotnet run
```

运行后：
- Swagger: `http://localhost:5180/swagger`
- 从 Android 模拟器访问：`http://10.0.2.2:5180`
- 若使用真机访问，请确保防火墙放行 5180 端口，并使用你的主机局域网 IP（例如 `http://192.168.x.x:5180`）

---

### 3. 数据库准备（MySQL）
若不使用内存数据库，请在 MySQL 中执行以下 SQL（示例以 UTF8MB4 为例）：

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS maca
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE maca;

-- 用户表
CREATE TABLE IF NOT EXISTS Users (
  Id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  Username VARCHAR(100) NOT NULL,
  Password VARCHAR(255) NOT NULL,
  UserType VARCHAR(50) NOT NULL,
  UNIQUE KEY UX_Users_Username (Username)
) ENGINE=InnoDB;

-- 广告表（仅存放文件名，文件需放在 E:\ads 或你配置的静态目录中）
CREATE TABLE IF NOT EXISTS Ads (
  Id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ImageFileName VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

-- 分数表（无 CreatedAt 列；代码中该属性会被 EF 忽略，不会写入）
CREATE TABLE IF NOT EXISTS Scores (
  Id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  UserId INT NOT NULL,
  Score INT NOT NULL,
  CONSTRAINT FK_Scores_Users_UserId FOREIGN KEY (UserId)
    REFERENCES Users(Id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 种子数据（可选）
INSERT INTO Users (Username, Password, UserType) VALUES
  ('admin', 'admin', 'Paid User'),
  ('guest', 'guest', 'Free User')
ON DUPLICATE KEY UPDATE Username=VALUES(Username);

-- 示例广告文件名（请确保文件已存在于静态目录）
INSERT INTO Ads (ImageFileName) VALUES
  ('ad1.jpg'), ('ad2.jpg'), ('ad3.png');
```

设置连接串（示例）：
```
Server=localhost;Database=maca;User=root;Password=你的密码;
```

---

### 4. Android 前端配置与运行
前端工程路径：`AndroidApp/`

关键点：
- 基础地址默认指向 Android 模拟器访问宿主机的别名 `10.0.2.2:5180`
- 已允许明文 HTTP 与用户证书（`res/xml/network_security_config.xml`）
- 使用 Retrofit + Gson

#### 4.1 BASE_URL（如需更改）
文件：`AndroidApp/app/src/main/java/com/team06/maca/network/ApiService.kt`

```kotlin
private const val BASE_URL = "http://10.0.2.2:5180/"
```

如果：
- 你使用真机，且后端运行在同一局域网另一台机器上：请将 `BASE_URL` 改为 `http://<后端主机局域网IP>:5180/`
- 你更改了后端端口：请同步修改端口

此外，后端 `AdsController` 也内置了用于拼接广告 URL 的 `baseUrl = "http://10.0.2.2:5180"`。若前端 BASE_URL 改为其它主机/端口，建议同步调整后端该常量或改造为配置项，确保广告图片 URL 指向正确后端主机与端口。

#### 4.2 运行步骤
1) 打开 Android Studio，File > Open 选择 `AndroidApp/`
2) 等待 Gradle 同步完成
3) 启动 Android 模拟器（建议 Android 11+）
4) 先运行后端（确保 `http://10.0.2.2:5180` 可达）
5) 在 Android Studio 运行 App

登录测试账号（若用内存库或你已按上文导入数据）：
- `admin/admin`（付费用户）
- `guest/guest`（免费用户）

---

### 5. 可用 API（摘录）
- 登录：`POST /api/auth/login`，Body: `{"username": "...", "password": "..."}`，返回 `{ userType, token }`
- 获取图片：`GET /api/game/images?count=20`，返回 `{ images: [...] }`
- 获取广告：`GET /api/ads/next`，返回 `{ adUrl }`（文件来自静态目录映射 `/ad-images`）
- 提交分数（需认证）：`POST /api/scores`，Header: `Authorization: Bearer <token>`，Body: `{"score": 123}`
- 排行榜 Top5：`GET /api/leaderboard/top5`，返回 `[{ user, score }, ...]`

Swagger（开发）可查看详细契约：`http://localhost:5180/swagger`

---

### 6. 常见问题与排错
- 无法获取广告（404）：
  - 检查 MySQL 中 `Ads` 表是否有记录，且文件已存在 `E:\ads`（或你配置的静态目录）
  - 若你修改了静态目录路径，记得同步改 `Program.cs` 中的 `PhysicalFileProvider(...)`
- 分数提交返回 401：
  - 需要先登录并在后续请求中带上 `Authorization: Bearer <token>`
  - 确认前端 `RemoteRepository` 已缓存到 token 并拼接了 `Bearer `
- Android 端访问 `localhost` 无响应：
  - 模拟器需使用 `10.0.2.2` 访问宿主机
  - 真机需使用宿主机的局域网 IP（如 `192.168.x.x`），并放行防火墙端口 5180
- 连接数据库失败：
  - 核对连接串（主机、端口、数据库、用户、密码）
  - 确认 MySQL 服务已启动且可通过客户端连接
- JWT 配置异常：
  - `Jwt:Key` 必填，长度足够，建议 32+ 字符随机串
  - 如配置了 Issuer/Audience，前后端需一致

---

### 7. 团队协作与安全建议
- 别将真实密码与 JWT Key 直接提交到仓库。建议：
  - 使用 `appsettings.Development.json`（仅本地）或环境变量提供敏感配置
  - 为不同环境（开发/测试/生产）准备不同配置文件
- 若团队中有人无法使用 `E:\ads` 路径：
  - 统一在代码中引入可配置项（如通过 `appsettings.json` 指定广告目录），避免硬编码路径

---

### 8. 目录结构（摘录）
```
AndroidApp/
  app/src/main/java/com/team06/maca/
    network/ApiService.kt        # BASE_URL
    repository/RemoteRepository.kt
    ...

Backend/MemoryGameBackend/
  Controllers/                   # Auth, Game, Ads, Score
  Data/GameDbContext.cs          # EF Core 映射（Users/Scores/Ads）
  Models/                        # 实体
  Program.cs                     # 端口/静态目录/认证/DB 配置
  appsettings.json               # 连接串与 JWT
```

---

### 9. 运行顺序（TL;DR）
1) （可选）按第 3 节建库、建表并导入示例数据；或使用第 2.3 节内存数据库快速跑通
2) 配置后端 `appsettings.json`（尤其是 `Jwt:Key` 与 `ConnectionStrings`）
3) 创建并填充 `E:\ads`（或修改静态目录路径）
4) 运行后端：`dotnet run`（访问 `http://localhost:5180/swagger`）
5) 运行 Android 应用（模拟器使用 `10.0.2.2:5180`）
6) 使用 `admin/admin` 或 `guest/guest` 登录，验证广告/图片/排行/提交分数


