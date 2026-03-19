# 企业信息化平台部署参考文档（等保 2.0 合规版）

## 1. 概述
本文档描述了基于 JDK 17 和 Spring Boot 3.4 构建的企业信息化平台的部署架构。为满足《网络安全等级保护基本要求2.0》（等保2.0）的强合规监管，本部署方案严格贯彻“一个中心，三重防御”的纵深防御体系。文档包含基于等保要求的服务器资源分配建议、环境划分以及合规的持续集成与持续交付（CI/CD）流程设计。

## 2. 部署环境规划与安全隔离

为保证系统稳定性与开发效率，并满足等保关于“测试数据与生产数据严格隔离”的要求，平台划分为四个主要的部署环境，环境之间必须实现网络级物理或严格的逻辑隔离（VPC 对等连接管控）：

### 2.1 开发环境 (DEV)
* **用途**: 开发人员日常代码编写、联调、初步自测。
* **特点**: 频繁更新，可用性要求不高。**严禁导入未经脱敏的生产数据**。
* **访问控制**: 仅限于开发团队内部访问（强制通过公司 VPN 接入）。

### 2.2 测试环境 (TEST/QA)
* **用途**: 质量保证团队（QA）进行集成测试、系统测试、性能压测及自动化回归测试。
* **特点**: 环境稳定性高于 DEV，配置架构贴近生产。**数据必须使用专门的模糊测试伪造数据**。
* **访问控制**: 开发、测试人员通过堡垒机访问，不向最终用户开放。

### 2.3 预发布环境 (PRE/UAT)
* **用途**: 用户验收测试（User Acceptance Testing），生产发布前的最后一环。
* **特点**: 架构、配置与生产环境保持完全一致。使用生产级别的负载均衡和安全策略。
* **访问控制**: 少数内部用户、产品经理进行业务走查，**严格执行数据防泄露与审计**，不对公众开放。

### 2.4 生产环境 (PROD)
* **用途**: 正式对外提供服务，承载企业核心商业机密。
* **特点**: 极高的可用性、稳定性、安全性要求。所有变更必须经过严格的线上工单审批。
* **等保合规要求**: 具备完整的态势感知、入侵防御、防病毒、落盘加密与 180 天以上的全量日志审计闭环。运维人员必须通过双因素认证（MFA）与堡垒机接入。

## 3. 服务器资源分配与等保物理部署架构

依据等保 2.0 对“安全区域边界”、“安全通信网络”、“安全计算环境”的纵深要求，以下为 PROD 环境的部署架构。

### 3.1 物理部署架构图 (Deployment Architecture Diagram)
物理部署架构图展示了软件组件如何映射到实际的云资源上，并严格标注了等保合规的安全域划分，用于直接输出云资源的采购清单（BOM表）。

```mermaid
flowchart TD
    Internet(["全球互联网公共网络"])

    subgraph Cloud["公有云/私有云基础设施"]
        subgraph AZA["可用区 A (跨可用区容灾设计)"]

            subgraph SecBoundary["等保防御层一：安全区域边界 (DMZ)"]
                EIP(["公网出口 / 弹性 EIP"])
                Anti_DDoS["高防 IP 洗流中心"]
                WAF["WAF / 云防火墙\n(七层拦截 & 流量审计)"]
                SLB["云负载均衡 SLB\n(按流量计费 或 独享实例)"]
            end

            subgraph SecComm["等保防御层二：安全通信网络 (VPC 隔离区)"]
                subgraph WebTier["内网反向代理区"]
                    Nginx1["Nginx 节点 1\n(ECS: 4核8G)"]
                    Nginx2["Nginx 节点 2\n(ECS: 4核8G)"]
                end

                subgraph AppTier["等保防御层三：安全计算环境 (应用微隔离)"]
                    App1["Spring Boot 节点 1\n(ECS: 8核16G, CWPP无代理防护)"]
                    App2["Spring Boot 节点 2\n(ECS: 8核16G, CWPP无代理防护)"]
                    AppN["Spring Boot 节点 N\n(按需弹性伸缩)"]
                end

                subgraph DataTier["等保防御层三：核心数据计算环境 (透明加密)"]
                    MySQL_Master[("MySQL 主库\n(16核64G, 高IOPS, 落盘加密)")]
                    MySQL_Slave1[("MySQL 从库 1\n(8核32G, 只读分离)")]
                    MySQL_Slave2[("MySQL 从库 2\n(8核32G, 只读分离)")]

                    Redis[("Redis Cluster\n(8核32G集群, 强密码认证)")]
                    Kafka[("Kafka + ZK/KRaft\n(8核32G集群, 高吞吐磁盘)")]
                end
            end

            subgraph SecCenter["等保核心：安全管理中心 (独立 VPC)"]
                Bastion["堡垒机 (MFA认证)"]
                SOC["云安全中心/态势感知"]
                LogCenter["日志审计中心\n(存储周期 > 180天)"]
            end

            Internet --> EIP
            EIP --> Anti_DDoS
            Anti_DDoS --> WAF
            WAF --> SLB

            SLB --> Nginx1
            SLB --> Nginx2

            Nginx1 --> App1
            Nginx1 --> App2
            Nginx1 --> AppN

            Nginx2 --> App1
            Nginx2 --> App2
            Nginx2 --> AppN

            App1 -.-> MySQL_Master
            App1 -.-> Redis
            App1 -.-> Kafka

            App2 -.-> MySQL_Master
            App2 -.-> Redis
            App2 -.-> Kafka

            MySQL_Master -.->|Binlog同步| MySQL_Slave1
            MySQL_Master -.->|Binlog同步| MySQL_Slave2

            Bastion -.->|带外运维网络| SecBoundary
            Bastion -.->|带外运维网络| SecComm
            SOC -.->|全网态势采集| SecBoundary
            SOC -.->|配置基线扫描| SecComm
            LogCenter -.->|流量与审计汇聚| SecComm
        end
    end
```

以下是一个中等规模企业级应用的基础资源分配方案，可根据实际 QPS、并发量和数据量进行横向扩展（Scale-out）或纵向扩展（Scale-up）。

**核心原则**: 数据库服务器重 CPU/内存/IOPS；应用服务器重 CPU/内存；缓存服务器重内存；消息队列重 IOPS/内存。

### 3.1 负载均衡与边界安全区
* **角色**: 高防IP / WAF / Nginx / HAProxy / 云原生负载均衡器 (SLB)
* **等保要求**: 必须具备抗 D/CC 攻击能力，开启 HTTPS 强加密传输，阻断恶意探测扫描。
* **规格建议**: 4核 CPU / 8GB 内存 / 50GB 高性能系统盘 / 高带宽网络出口 (如 50-100Mbps)
* **数量**: 至少 2 台 (主备/双活架构)

### 3.2 核心应用计算区
* **角色**: Spring Boot 3.4 后端应用 (利用 JDK 17 优化)
* **等保要求**: 工作负载部署端点防护平台（CWPP/云安全中心），执行严格的 OS 漏洞扫描与基线核查。
* **规格建议**: 8核 CPU / 16GB 或 32GB 内存 / 100GB 系统盘
* **数量**: 至少 3-4 台 (视微服务拆分和预估并发量而定，支持弹性扩缩容)
* **配置提示**: JVM 参数根据 JDK 17 的特性调整 (如 `-XX:+UseZGC` 或 G1，`-Xms8g -Xmx8g`)。

### 3.3 核心数据存储区
* **角色**: MySQL 8.0+ (一主多从)
* **等保要求**: 数据必须支持跨节点备份，开启 KMS 落盘透明加密，数据库运维全量计入审计日志。严禁直接绑定公网 IP。
* **规格建议 (主库)**: 16核 CPU / 64GB 内存 / 500GB+ SSD 极速云盘 (关注高 IOPS)
* **规格建议 (从库)**: 8核 CPU / 32GB 内存 / 500GB+ SSD (承载只读流量)
* **数量**: 至少 1 主 2 从

### 3.4 分布式缓存区
* **角色**: Redis Cluster
* **等保要求**: 禁止绑定外网，配置复杂的访问鉴权凭证，开启日志并定期修改密码。
* **规格建议**: 8核 CPU / 32GB 内存 / 100GB 系统盘
* **数量**: 至少 3 主 3 从 (部署在至少 3 台物理节点上)

### 3.5 消息队列中间件区
* **角色**: Apache Kafka + Zookeeper / KRaft
* **规格建议**: 8核 CPU / 32GB 内存 / 100GB系统盘 + 500GB 数据盘 (高 IO 吞吐)
* **数量**: 至少 3 台构建高可用集群

### 3.6 安全管理中心 (带外运维区)
* **角色**: 堡垒机, SOC态势感知, 日志审计系统(ELK)
* **等保要求**: 这是等保2.0的“一中心”核心，所有日常管理动作与业务流量物理/逻辑分离。
* **规格建议**:
  * 堡垒机 (MFA必须): 2核 / 4GB
  * ELK (日志保留期>180天): 至少 3 台 (16核 / 32GB / T级别大容量低频存储盘)

## 4. CI/CD (持续集成与安全交付 / DevSecOps)

为从源头上阻断安全隐患，部署流程必须由传统的 CI/CD 升级为融入“安全左移”理念的 DevSecOps 流水线。

### 4.1 DevSecOps 总体流程
代码提交 (云效代码管理) -> 触发构建 (云效流水线) -> **核心卡点: 静态代码扫描 (SAST) + 敏感凭证防泄漏 (AK/SK) (云效代码检测)** -> 单元测试 -> 构建 Docker 镜像 -> **核心卡点: 容器镜像 CVE 漏洞扫描** -> 推送镜像仓库 (制品仓库Packages) -> 严格工单审批触发部署 -> Kubernetes/Swarm 拉取镜像更新服务 -> 自动化验证与态势监控。

### 4.2 Spring Boot 3.4 与 JDK 17 的部署优化
由于使用了 Spring Boot 3.x，强烈建议采用容器化部署（Docker/Kubernetes）。

1. **多阶段构建 (Multi-stage Build)**:
   在 `Dockerfile` 中使用一个包含 JDK/Maven(或Gradle) 的基础镜像进行编译打包，再将产出的 JAR 拷贝到一个仅包含 JRE 17 (如 Alpine 瘦身版) 的极简运行环境镜像中。此举不仅大幅减小最终镜像体积，更能显著降低底层 OS 库的暴露攻击面（缩小 CVE 漏洞基数）。
2. **Spring Boot Native Image (可选)**:
   Spring Boot 3.x 深度整合了 GraalVM，可通过 AOT 编译生成 Native Image（原生二进制可执行文件）。
   * 优势：启动时间极快（毫秒级），内存占用极低。由于移除了大量未使用的 JVM 特性，安全攻击面进一步收敛。
   * 劣势：编译时间长，部分动态特性（如复杂的反射、CGLib代理）可能需要额外配置。
3. **分层 JAR (Layered Jar)**:
   利用 Spring Boot 的 Layered Jar 特性，在构建 Docker 镜像时将依赖库（dependencies）和业务代码（application）分层打包。由于依赖库变动频率低，可充分利用 Docker 缓存，加快持续集成的构建速度。

### 4.3 自动化运维与部署工具链
本平台依托阿里云云效作为官方 DevSecOps 工具链：
* **代码托管**:  [云效代码管理](https://codeup.aliyun.com/?navKey=mine) (开启离职人员自动撤权与防泄漏审计)
* **构建与发布**:  [云效流水线](https://flow.aliyun.com/my?page=1)
* **代码审查与质量安全门禁**:  [云效代码检测](https://codeup.aliyun.com/checks/tasks)
* **容器镜像仓库**:  [制品仓库Packages](https://packages.aliyun.com/) (强制开启镜像推送后自动查杀扫描)

### 4.4 滚动更新与灰度发布机制
* 平台必须支持**零宕机无损部署**。在 Kubernetes 中通过配置 `Deployment` 的 `strategy.type: RollingUpdate` 来实现滚动更新。
* 核心变更需采用**细粒度金丝雀灰度发布**，通过 Ingress 或 Gateway 网关路由规则，将极少部分真实流量（如 5%）导入新版本服务，在 APM 监控未发现 5xx 错误或 CPU 异动后再全量发布。一旦识别到性能劣化或安全告警，支持一键秒级回滚。