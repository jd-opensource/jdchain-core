## RocksDB as a server

### 简介

`KVDB`是一个简单的`NoSQL`数据库，支持简单的“键值”读写操作。

`KVDB`包装了`RocksDB`作为数据库引擎，实现了单机部署和集群部署。

`KVDB`的集群是一个分布式的分片服务集群，每个分片节点是一个`KVDB`的数据库服务实例，采用对等模式部署，没有主节点。

### 安装

> java 版本>= 1.8 

下载源代码，执行：
```bash
mvn clean package
```
`kvdb-server`模块`target`下会生成`kvdb-***.zip`，解压缩安装包，结构如下：
```bash
bin # 脚本文件目录
  start # 启动本机的数据库服务的脚本文件
  stop # 停止本机的数据库服务的脚本文件
  kvdb-cli # 连接本机的数据库服务控制台的脚本文件
  kvdb-benchmark # 基准测试
conf # 配置文件目录
  kvdb.conf # 本机的数据库服务配置
  cluster.conf # 数据库集群的配置
libs # 数据库服务的代码库目录
system # 系统数据目录
  dblist # 文件记录了本数据库服务实例装载的数据库列表
  pid # 文件记录了本数据库服务实例的运行进程ID；当服务实例运行时创建，服务实例退出后删除
```

### 部署配置

#### `kvdb.conf`

```bash
# 数据库服务的本机监听地址；
server.host=0.0.0.0

# 数据库服务的本机监听端口；
server.port=7078

# 管理控制台的端口；
# 注：管理控制台总是绑定到环回地址 127.0.0.1，只允许本机访问；
manager.port=7060

# 数据库实例默认的根目录
dbs.rootdir=/usr/kvdb/dbs

# 数据库实例默认的本地分区数
dbs.partitions=4
```

#### `cluster.conf`
```bash
# 数据库集群的分片数，每一个分片都赋予唯一的编号，分片编号最小为 0，所有分片的编号必须连续递增
# ‘<name>’表示集群名称，只允许用字母、数字、下划线组成；
cluster.<name>.partitions=3

# 数据库集群 ‘<name>’ 的第 1 个分片的数据库实例地址(URL格式)；
cluster.<name>.0=kvdb://<host>:<port>/<dbname>

# 数据库集群 ‘<name>’ 的第 2 个分片的数据库实例地址(URL格式)；
cluster.<name>.1=kvdb://<host>:<port>/<dbname>

# 数据库集群 ‘<name>’ 的第 3 个分片的数据库实例地址(URL格式)；
cluster.<name>.2=kvdb://<host>:<port>/<dbname>

# 指定多个不同的集群
#cluster.<name1>.partitions=3
...
```
> 一个数据库实例只能加入唯一的集群；一旦加入集群，则不能再被客户端以单库连接方式访问，对该库实例的连接自动转为集群连接。集群中配置的数据库必须在`dblist`中已存在且设置为`true`

#### `dblist`
```bash
# 是否激活数据库 ‘<name>’ ；如果为`true`，则创建或加载该数据库并提供访问；
# ‘<name>’表示数据库名称，只允许用字母、数字、下划线组成；
# db.<name>.enable=true

# 数据库 <name> 的根目录；如果未配置，则从默认目录加载(由`conf/kvdb.conf`指定了默认配置${dbs.rootdir}/<name>)；
# db.test.rootdir=

# 数据库 <name> 的本地分区数；如果未配置，则采用`conf/kvdb.conf`中指定的默认配置${dbs.partitions}
# db.test.partitions=
```
> `dblist`中配置的数据库会在`kvdb-server`启动时自动创建。在`kvdb-server`启动完成后由管理工具执行`create database <name>`创建数据库，创建成功的数据库会追加到`dblist`中。使用`kvdb-cli`命令行工具执行数据库实例更新操作。

#### 日志

1. `kvdb-server`

修改`bin`目录下，`start.sh`文件：
```bash
LOG_SET="-Dlogging.path="$HOME/logs" -Dlogging.level=error"
```
默认日志路径：程序解压缩后主目录下`logs`目录
默认日志等级：`error`

2. `kvdb-cli`

修改`bin`目录下，`kvdb-cli.sh`文件：
```bash
LOG_SET="-Dlogging.path="$HOME/logs" -Dlogging.level.root=error"
```
默认日志路径：程序解压缩后主目录下`logs`目录
默认日志等级：`error`

#### 启动

```bash
./start.sh
```
#### 停止
```bash
./stop.sh
```

### SDK

1. 依赖
```maven
<dependency>
	<groupId>com.jd.blockchain</groupId>
	<artifactId>kvdb-client</artifactId>
	<version>${version}</version>
</dependency>
```

2. 创建客户端连接
```java
// `host`、`port`为服务器地址和端口，`database`为数据库名称
KVDBClient client = new KVDBClient("kvdb://<host>:<port>/<database>");
```

3. 操作

```java
/**
 * 存在性查询
 *
 * @param key
 * @return
 * @throws KVDBException
 */
boolean exists(Bytes key) throws KVDBException;

/**
 * 存在性查询，支持多个键值
 *
 * @param keys
 * @return
 * @throws KVDBException
 */
boolean[] exists(Bytes... keys) throws KVDBException;

/**
 * 键值获取
 *
 * @param key
 * @return
 * @throws KVDBException
 */
Bytes get(Bytes key) throws KVDBException;

/**
 * 键值获取，支持多个键值
 *
 * @param keys
 * @return
 * @throws KVDBException
 */
Bytes[] get(Bytes... keys) throws KVDBException;

/**
 * 设置键值对
 *
 * @param key
 * @param value
 * @return
 * @throws KVDBException
 */
boolean put(Bytes key, Bytes value) throws KVDBException;

/**
 * 开启批处理
 *
 * @return
 * @throws KVDBException
 */
boolean batchBegin() throws KVDBException;

/**
 * 取消批处理
 *
 * @return
 * @throws KVDBException
 */
boolean batchAbort() throws KVDBException;

/**
 * 提交批处理，服务器掉线重连后会丢失未提交批处理数据
 * <p>
 * 未提交的`batch`对其他客户端连接不可见。
 *
 * @return
 * @throws KVDBException
 */
boolean batchCommit() throws KVDBException;

/**
* 关闭客户端
*/
public void close();
```

### 管理工具

`kvdb-cli`是基于[`SDK`](#SDK)的命令行工具实现：

```bash
./kvdb-cli.sh -h <kvdb server host> -p <kvdb server port> -db <database> -t <time out in milliseconds>  -rt <retry times for time out> -bs <buffer size> -k <keep alive>
```
参数说明：

- `-h` 服务器地址。选填，默认`localhost`
- `-p` 端口。选填，默认`6070`
- `-db` 数据库。选填
- `-t` 超时时间，毫秒。选填，默认`60000 ms`
- `-rt` 超时重试等待次数。选填，默认`5`
- `-bs` 发送/接收缓冲区大小。选填，默认`1024*1024`
- `-k` 保持连接。选填，默认`true`

所有支持指令操作：
```bash
localhost:7060>help
AVAILABLE COMMANDS

Built-In Commands
        clear: Clear the shell screen.
        exit, quit: Exit the shell.
        help: Display help about available commands.
        stacktrace: Display the full stacktrace of the last error.

KVDB Commands
        batch abort: 取消批处理
        batch begin: 开启批处理
        batch commit: 提交批处理
        cluster info: 服务器集群配置信息
        create database: 创建数据库实例
        disable database: 关闭数据库实例，加入集群的实例不可修改
        drop database: 删除数据库实例，加入集群的实例不可修改
        enable database: 开放数据库实例，加入集群的实例不可修改
        exists: 检查存在性
        get: 获取键值
        put, set: 设置键值
        show databases: 展示数据库实例列表
        status: 当前数据库信息
        use: 切换数据库
```

指令帮助（以`put`为例）：
```bash
localhost:7060>help put

NAME
	put - Set a key-value

SYNOPSYS
	put [--key] string  [--value] string

OPTIONS
	--key  string

		[Mandatory]

	--value  string

		[Mandatory]

ALSO KNOWN AS
	set

```

示例（可执行如下指令插入数据）：
```bash
localhost:7060>put k v
localhost:7060>set k v
localhost:7060>put --key k --value v
localhost:7060>set --key k --value v
```

### Benchmark

`kvdb-sever`性能测试工具，简单的数据插入测试。

```bash
./kvdb-benchmark.sh -h <kvdb server host> -p <kvdb server port> -db <database> -c <time out in milliseconds>  -n <retry times for time out> -b <buffer size> -k <keep alive>
```
参数说明：

- `-h` 服务器地址。选填，默认`localhost`
- `-p` 端口。选填，默认`7078`
- `-db` 数据库。必填
- `-c` 客户端数量。选填，默认`20`
- `-n` 请求数量。选填，默认`100000`
- `-b` 是否使用批处理。选填，默认`false`
- `-k` 保持连接。选填，默认`true`

示例：
```bash
./kvdb-benchmark.sh -db test1 -c 20 -n 1000000
requests:1000000, clients:20, batch:false, times:22113ms, errors:0, tps:45222.267444
```
其中：

- `requests` 请求数量
- `clients` 并发数
- `batch` 是否开启批量模式
- `times` 总耗时
- `tps` TPS

可根据`tps`值，调整`kvdb.conf`中`dbs.partitions`数值，以寻找最优服务器配置。