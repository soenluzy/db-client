# Database Client CLI

一个轻量级的数据库命令行客户端，支持 MySQL 和 PostgreSQL，通过交互式 REPL 执行 SQL 查询和数据导出。

## 功能特性

- **双数据库支持** - MySQL 和 PostgreSQL
- **交互式 REPL** - 类似 psql 的命令行交互体验
- **表格输出** - 查询结果以格式化的 ASCII 表格展示
- **数据导出** - 支持 CSV 和 Excel (XLSX) 格式导出
- **多环境配置** - 通过配置文件管理多个数据库连接
- **详细错误信息** - SQL 执行错误显示完整堆栈信息

## 环境要求

- Java 1.8+
- Maven 3.6+
- MySQL 5.7+ 或 PostgreSQL 9.6+
- Linux/macOS/Windows

## 快速开始

### 构建项目

```bash
git clone <repository-url>
cd db-client
mvn clean package -DskipTests
```

构建完成后，JAR 文件位于 `target/db-client-1.0.0.jar`

### 创建配置文件

在 `~/.db-client/db-config.yaml` 创建配置文件：

```yaml
profiles:
  mysql-dev:
    type: mysql
    host: localhost
    port: 3306
    database: myapp_dev
    username: dev_user
    password: your_password

  pgsql-dev:
    type: postgresql
    host: localhost
    port: 5432
    database: myapp_dev
    username: dev_user
    password: your_password

  mysql-prod:
    type: mysql
    host: prod.example.com
    port: 3306
    database: myapp_prod
    username: prod_user
    password: your_password
```

### 运行

```bash
# 使用默认配置（第一个 profile）
java -jar target/db-client-1.0.0.jar

# 指定 profile
java -jar target/db-client-1.0.0.jar --profile mysql-dev

# 指定配置文件路径
java -jar target/db-client-1.0.0.jar --config /path/to/config.yaml --profile pgsql-dev
```

## 使用说明

### REPL 命令

| 命令 | 说明 |
|------|------|
| `SELECT ...` | 执行 SQL 查询 |
| `INSERT ...` | 执行数据插入 |
| `UPDATE ...` | 执行数据更新 |
| `DELETE ...` | 执行数据删除 |
| `\export <path>` | 导出上次查询结果到 CSV/XLSX |
| `\connect <profile>` | 切换数据库连接 |
| `\help` | 显示帮助信息 |
| `\exit` | 退出 REPL |

### 使用示例

```
$ java -jar target/db-client-1.0.0.jar --profile mysql-dev

Database Client REPL
Type \help for available commands

[MYSQL] myapp_dev> SELECT * FROM users LIMIT 5;
+----+--------+------------------+
| id | name   | email            |
+----+--------+------------------+
| 1  | Alice  | alice@example.com|
| 2  | Bob    | bob@example.com  |
| 3  | Charlie| charlie@ex.com   |
+----+--------+------------------+
(3 rows in 15ms)

[MYSQL] myapp_dev> \export /tmp/users.csv
Exported to /tmp/users.csv

[MYSQL] myapp_dev> \export /tmp/users.xlsx
Exported to /tmp/users.xlsx

[MYSQL] myapp_dev> \connect pgsql-dev
Connected to pgsql-dev

[POSTGRESQL] myapp_dev> SELECT count(*) FROM users;
+-------+
| count |
+-------+
| 1000  |
+-------+
(1 rows in 8ms)

[POSTGRESQL] myapp_dev> \exit
Goodbye!
```

### 快捷键

| 快捷键 | 说明 |
|--------|------|
| `Ctrl+C` | 取消当前输入 |
| `Ctrl+D` | 退出 REPL |
| `↑/↓` | 浏览历史命令 |

## 配置文件说明

### 配置文件位置

1. `--config` 参数指定的路径
2. `~/.db-client/db-config.yaml`
3. classpath 中的 `db-config.yaml`

### 配置字段

| 字段 | 必填 | 说明 |
|------|------|------|
| `type` | 是 | 数据库类型：`mysql` 或 `postgresql` |
| `host` | 是 | 数据库主机地址 |
| `port` | 是 | 数据库端口 |
| `database` | 是 | 数据库名称 |
| `username` | 是 | 用户名 |
| `password` | 是 | 密码 |

## 项目结构

```
db-client/
├── pom.xml
└── src/main/java/com/example/dbclient/
    ├── Main.java                    # 程序入口
    ├── cli/
    │   ├── Repl.java                # REPL 主循环
    │   └── CommandHandler.java      # 命令处理
    ├── config/
    │   ├── ConfigLoader.java        # 配置加载
    │   └── DatabaseProfile.java     # 配置模型
    ├── db/
    │   ├── DatabaseType.java        # 数据库类型枚举
    │   ├── DatabaseManager.java     # 数据库连接管理
    │   └── QueryResult.java         # 查询结果封装
    ├── export/
    │   ├── Exporter.java            # 导出接口
    │   ├── CsvExporter.java         # CSV 导出
    │   ├── ExcelExporter.java       # Excel 导出
    │   └── ExporterFactory.java     # 导出工厂
    └── formatter/
        └── TableFormatter.java      # 表格格式化
```

## 技术栈

| 组件 | 库 |
|------|-----|
| MySQL 连接 | MySQL Connector/J 8.0.33 |
| PostgreSQL 连接 | PostgreSQL JDBC 42.7.1 |
| REPL 交互 | JLine 3.25.1 |
| Excel 导出 | Apache POI 5.2.5 |
| CSV 导出 | Apache Commons CSV 1.10.0 |
| 配置解析 | SnakeYAML 2.2 |

## 注意事项

1. **密码安全** - 配置文件中的密码为明文，建议设置文件权限为 600
2. **连接池** - 当前版本每次连接创建新连接，不支持连接池
3. **大数据导出** - 导出功能使用流式处理，内存占用低，支持较大数据集
4. **事务** - 当前版本自动提交，不支持事务管理

## 后续规划

- [ ] 命令历史持久化
- [ ] SQL 自动补全
- [ ] SSL/TLS 连接支持
- [ ] 查询结果分页展示
- [ ] 连接池支持

## 许可证

MIT License