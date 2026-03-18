# 企业信息化平台技术架构文档

## 1. 概述
本文档旨在为企业信息化平台提供基于业界最佳实践的标准技术架构。本平台基于 **JDK 17**、**Spring Boot 3.4** 等前沿技术栈构建，旨在提供高可用、可扩展、安全可靠的企业级服务。

## 2. 核心技术栈选型

### 2.1 后端技术栈
* **编程语言**：Java 17 (利用新特性如Record, 增强的Switch, ZGC等，提升性能和开发效率)
* **核心框架**：Spring Boot 3.4 (原生支持Jakarta EE 10, AOT编译和虚拟线程支持)
* **微服务治理/服务间通信**：Spring Cloud (如需微服务拆分) / RESTful API / gRPC
* **ORM框架**：MyBatis-Plus 或 Spring Data JPA
* **权限认证**：Spring Security + OAuth2.0 / JWT
* **API 文档**：Springdoc OpenAPI (Swagger 3)

### 2.2 数据存储与中间件
* **关系型数据库**：MySQL 8.0+ (提供更好的JSON支持，窗口函数，性能提升)
* **分布式缓存**：Redis (用于热点数据缓存、分布式锁、会话共享)
* **消息队列**：Apache Kafka (处理高吞吐量异步消息、削峰填谷、日志收集)
* **搜索引擎 (可选)**：Elasticsearch (用于复杂全文检索、日志聚合分析)
* **对象存储**：MinIO / 阿里云OSS / 腾讯云COS (非结构化数据如图片、文档存储)

### 2.3 前端技术栈 (建议)
* **核心框架**：Vue 3 或 React 18
* **UI 组件库**：Element Plus (Vue) / Ant Design (React)
* **状态管理**：Pinia (Vue) / Redux Toolkit (React)
* **构建工具**：Vite

### 2.4 运维与部署技术栈
* **容器化**：Docker
* **容器编排**：Kubernetes (K8s) (推荐用于生产环境的高可用和弹性伸缩)
* **CI/CD**：GitLab CI / Jenkins / GitHub Actions
* **网关与反向代理**：Nginx / Spring Cloud Gateway
* **监控与告警**：Prometheus + Grafana
* **链路追踪**：SkyWalking / Jaeger
* **集中式日志**：ELK Stack (Elasticsearch, Logstash/Filebeat, Kibana) / EFK

## 3. 系统架构设计

### 3.1 逻辑架构图 (Logical Architecture Diagram)
这是架构选型最核心的一张图。它从逻辑上将系统分层，帮助技术团队明确各个层次的技术选型和职责。

```mermaid
flowchart TD
    subgraph Presentation["展示层 (Presentation Layer)"]
        direction LR
        Web["PC Web端\n(Vue 3/React 18)"]
        Mobile["移动端 H5/APP\n(Taro/UniApp)"]
        WeChat["微信小程序\n(微信原生/UniApp)"]
    end

    subgraph Access["接入层 (Access Layer)"]
        SLB["云厂商 SLB / WAF"] --> Nginx["Nginx 集群\n(动静分离)"]
    end

    subgraph Gateway["网关层 (API Gateway Layer)"]
        SCG["Spring Cloud Gateway / ShenYu\n(鉴权认证: Spring Security+JWT | 统一路由 | 限流熔断 | 日志审计)"]
    end

    subgraph Business["业务服务层 (Business Layer - JDK 17 + Spring Boot 3.4)"]
        direction LR
        UserService["用户服务"]
        AuthService["权限服务"]
        OrderService["订单服务"]
        MessageService["消息服务"]
        ConfigService["基础配置"]
        ReportService["报表服务"]
        WorkflowService["工作流服务"]
        PayService["支付服务"]
    end

    subgraph Middleware["中间件与支撑层 (Middleware Layer)"]
        direction LR
        Redis["Redis缓存"]
        Kafka["Kafka消息队列"]
        XXLJob["XXL-JOB调度"]
        ES["Elasticsearch"]
    end

    subgraph Data["数据存储层 (Data Layer)"]
        direction LR
        MySQL["MySQL 8.0+ 集群\n(主从/读写分离)"]
        OSS["MinIO / 阿里云OSS\n(非结构化文件存储)"]
    end

    %% Connections
    Presentation -- HTTP/HTTPS --> Access
    Nginx -- RESTful API --> Gateway
    Gateway -- RPC / HTTP --> Business
    Business -.-> Middleware
    Business -.-> Data
    Middleware -.-> Data
```

### 3.2 系统上下文图 (System Context Diagram)
系统上下文图帮助评估接口对接的复杂度，明确本系统与外部系统的交互边界。

```mermaid
flowchart LR
    OldSystem["原有旧业务系统\n(PHP/C#)\n需考虑重构/数据同步"]

    subgraph Core["核心系统"]
        Platform["本企业信息化系统\n(Java/Spring Boot)"]
    end

    ThirdPartyPay["第三方支付平台\n(支付宝/微信支付)"]
    SMSGateway["短信/邮件网关\n(阿里云/腾讯云)"]
    InternalCollab["企业内部协同平台\n(企业微信/钉钉)"]

    OldSystem <-->|HTTP API| Platform
    Platform -->|HTTPS API| ThirdPartyPay
    Platform -->|HTTPS API| SMSGateway
    Platform -->|OAuth / API| InternalCollab
```

## 4. 网络拓扑规划 (Network Topology)

为了兼顾安全与性能，网络层面通常采用**VPC (Virtual Private Cloud)** 进行逻辑隔离。

### 4.1 网络分区设计
* **公网接入区 (DMZ)**:
  * 包含云防火墙 (WAF)、公共负载均衡器 (SLB/ALB)。
  * 仅开放 80 (HTTP) 和 443 (HTTPS) 端口。
* **应用/服务区 (内网)**:
  * 部署 Nginx、API Gateway、各个 Spring Boot 后端服务节点、前端静态资源服务器。
  * 仅允许来自 DMZ 区的特定流量访问。不对外网直接暴露。
* **数据与中间件区 (核心内网)**:
  * 部署 MySQL、Redis、Kafka 等。
  * 这是最核心的安全防线，仅允许应用服务区的主机在特定端口进行访问。
  * 数据库禁止分配公网 IP。
* **运维管理区**:
  * 部署堡垒机/跳板机、GitLab、CI/CD 服务器、Prometheus、日志收集等。
  * 运维人员通过 VPN 接入内网，再经过堡垒机访问其他各区服务器进行运维操作。

### 4.2 拓扑图示意 (Network Topology Diagram)

```mermaid
flowchart TD
    Internet["互联网 Internet"]

    subgraph DMZ["DMZ区 (分配公网IP)"]
        WAF["云防火墙 WAF / DDoS 防护"]
        SLB["云负载均衡 SLB"]
    end

    subgraph VPC["私有网络 (VPC)"]
        subgraph AppZone["应用服务区 (Subnet 1 - 无公网IP)"]
            Nginx["前端服务 / 反向代理\n(Nginx)"]
            Gateway["API 网关\n(Spring Cloud Gateway)"]
            SpringBoot["后端应用服务集群\n(Spring Boot Apps)"]
        end

        subgraph DataZone["数据与中间件区 (Subnet 2 - 最高安全级别)"]
            DB["数据库集群\n(MySQL 主从)"]
            Cache["缓存集群\n(Redis Cluster)"]
            MQ["消息队列\n(Kafka Cluster)"]
        end

        subgraph OpsZone["运维管理区 (Subnet 3)"]
            Bastion["堡垒机\n(仅允许授权 VPN/IP 访问)"]
            CICD["CI/CD 服务器\n(Jenkins/GitLab)"]
            Monitor["监控告警系统\n(Prometheus+Grafana)"]
        end
    end

    Internet -- HTTPS: 443 --> WAF
    WAF --> SLB
    SLB -- 内网 HTTP --> Nginx
    Nginx --> Gateway
    Gateway --> SpringBoot

    SpringBoot -- 内网 RPC/JDBC --> DB
    SpringBoot -- Redis Protocol --> Cache
    SpringBoot -- 内网 TCP --> MQ

    Bastion -.-> AppZone
    Bastion -.-> DataZone
    OpsZone -.-> AppZone
```
