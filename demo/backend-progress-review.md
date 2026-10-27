# Spring Boot 后端实际开发进度检查

检查日期：2026-09-23。

本文依据实际代码，不依据 README 或开发计划。原检查阅读了全部 30 个主代码文件、2 个测试文件、`pom.xml` 和数据库配置；本次目录更新又读取了新出现的两个成员模型文件，当前共有 32 个主代码文件。检查过程未修改源代码或配置。

原检查执行 `mvn -o -DskipTests compile` 后确认编译失败：`CharacterService.java:[18,5] 缺少返回语句`。本次加入成员模型后重新编译，首先报错 `CampaignMember.java:[25,25] 找不到符号 STRING`；原来的 CharacterService 方法仍缺少返回语句。没有执行测试、启动服务或核对数据库实际表结构。下文的“已实现”表示代码存在，不等于已经通过运行验证。

**当前结论：Memo 已有完整 CRUD 代码，Campaign 只有创建和查询；CampaignMember 已有实体和角色枚举雏形；Character 主要停留在 DTO 和创建方法起步阶段，尚未实现持久化。项目当前无法通过编译。**

## 当前项目文件夹结构

以下为本次读取的项目目录。省略 `.git/` 和 Maven 生成的 `target/` 内容；`target/` 并非源代码。

```text
demo/
├── .github/
│   └── modernize/java-upgrade/
│       ├── .gitignore
│       └── hooks/scripts/
│           ├── recordToolUse.ps1
│           └── recordToolUse.sh
├── .mvn/wrapper/
│   └── maven-wrapper.properties
├── .vscode/
│   ├── launch.json
│   └── settings.json
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   ├── config/
│   │   │   │   └── WebConfig.java
│   │   │   ├── Controller/
│   │   │   │   ├── CampaignController.java
│   │   │   │   └── MemoController.java
│   │   │   ├── DTO/
│   │   │   │   ├── CampaignRepository.java
│   │   │   │   ├── CharacterCreateRequest.java
│   │   │   │   ├── CharacterFeatResponse.java
│   │   │   │   ├── CharacterItemInput.java
│   │   │   │   ├── CharacterItemResponse.java
│   │   │   │   ├── CharacterResponse.java
│   │   │   │   ├── CharacterSkillInput.java
│   │   │   │   ├── CharacterSkillResponse.java
│   │   │   │   ├── CharacterType.java
│   │   │   │   ├── CreateMemoRequest.java
│   │   │   │   ├── MemoResponse.java
│   │   │   │   ├── NpcAffinityInput.java
│   │   │   │   ├── NpcAffinityResponse.java
│   │   │   │   ├── SensitiveFieldResponse.java
│   │   │   │   └── UpdateMemoRequest.java
│   │   │   ├── Entity/
│   │   │   │   ├── Campaign.java
│   │   │   │   ├── CampaignMember.java
│   │   │   │   ├── CampaignMemberRole.java
│   │   │   │   └── Memo.java
│   │   │   ├── Exception/
│   │   │   │   ├── CampaignNotFoundException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── MemoNotFoundException.java
│   │   │   ├── Repository/
│   │   │   │   ├── CampaignRepository.java
│   │   │   │   └── MemoRepository.java
│   │   │   ├── Service/
│   │   │   │   ├── CampaignService.java
│   │   │   │   ├── CharacterService.java
│   │   │   │   └── MemoService.java
│   │   │   ├── DemoApplication.java
│   │   │   └── MemoCategory.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/java/com/example/demo/
│       ├── DemoApplicationTests.java
│       └── MemoServiceTest.java
├── .gitattributes
├── .gitignore
├── HELP.md
├── backend-progress-review.md
├── mvnw
├── mvnw.cmd
└── pom.xml
```

目录树显示的是文件存在性；文件名不能证明功能已经完成。当前没有 `application.yml` 或独立 SQL 文件。

## 一、目前已经完成的功能

| 模块 | 实际存在的代码 | 当前完成程度 |
| --- | --- | --- |
| Campaign | `Campaign`、两份 `CampaignRepository`、`CampaignService`、`CampaignController` | 创建、查询全部、按 ID 查询；没有更新和删除，没有专用 DTO |
| Memo | `Memo`、`MemoRepository`、`MemoService`、`MemoController` | CRUD 代码齐全，另有按 Campaign 查询列表 |
| CampaignMember | `CampaignMember`、`CampaignMemberRole` | 已有实体和 DM／PLAYER 枚举；尚无 Repository、Service、Controller |
| Memo DTO | `CreateMemoRequest`、`UpdateMemoRequest`、`MemoResponse` | 已接入 Controller，输入与输出分离 |
| Memo Validation | `name @NotBlank`、`category @NotNull` | 创建、更新接口均通过 `@Valid` 触发校验 |
| Exception Handling | 两个 NotFound 异常、`GlobalExceptionHandler` | 已定义 404、无效请求体 400、参数校验 400 的处理 |
| 配置 | `WebConfig`、`application.properties` | 已配置本地前端 CORS、PostgreSQL 数据源和 JPA |

### Campaign

代码：[CampaignController.java](src/main/java/com/example/demo/Controller/CampaignController.java)，第 23–37 行。

- `POST /api/campaigns`
- `GET /api/campaigns`
- `GET /api/campaigns/{id}`

[Campaign Entity](src/main/java/com/example/demo/Entity/Campaign.java) 只有 `id / name / description / gameDate`。Controller 直接接收和返回 Entity。Service 使用 Repository 保存和查询，没有更新、删除或业务校验。

### Memo

代码：[MemoController.java](src/main/java/com/example/demo/Controller/MemoController.java)，第 33–80 行。

- `POST /api/campaigns/{campaignId}/memos`
- `GET /api/campaigns/{campaignId}/memos`
- `GET /api/memos/{memoId}`
- `PUT /api/memos/{memoId}`
- `DELETE /api/memos/{memoId}`，成功返回 204。

[MemoService.java](src/main/java/com/example/demo/Service/MemoService.java) 已实现：

- 创建前检查 Campaign 是否存在。
- 查询单条、更新、删除前检查 Memo 是否存在。
- 保存名称、类别、描述与 Campaign 关联。
- 按 Campaign ID 查询列表。

Memo 类别包含 `NPC / SECRET / LOCATION / ORGANIZATION / ITEM / EVENT / CUSTOM`。类别中的 NPC 只是 Memo 分类，不代表已经存在 NPC 角色系统。

### 测试

[MemoServiceTest.java](src/test/java/com/example/demo/MemoServiceTest.java) 有两项 Mockito 单元测试：

- Campaign 不存在时，创建 Memo 抛出异常。
- Campaign 存在时，创建并保存 Memo。

[DemoApplicationTests.java](src/test/java/com/example/demo/DemoApplicationTests.java) 有一个 `contextLoads()` 测试。本次编译失败，因此未执行测试。

## 二、目前正在开发但没有完成的功能

| 模块 | 已写内容 | 缺少内容 |
| --- | --- | --- |
| Character | `CharacterCreateRequest`、`CharacterResponse`、`CharacterType`、`CharacterService` | Entity、Repository、Controller、保存和返回逻辑 |
| CampaignMember / 成员系统 | `CampaignMember`、`CampaignMemberRole` | Repository、Service、Controller、成员创建与查询、归属及角色业务校验；没有 User 实体 |
| Skill | `CharacterSkillInput`、`CharacterSkillResponse` | Skill Entity、Repository、查询和校验逻辑 |
| CharacterSkill | 输入中的技能 ID、数值、unknown 字段 | 关联实体、数据库关系、约束和保存逻辑 |
| CharacterItem | `CharacterItemInput`、`CharacterItemResponse` | Entity、Repository、角色关联和保存逻辑 |
| NPC Affinity | `NpcAffinityInput`、`NpcAffinityResponse` | Entity、目标角色校验、关系映射和保存逻辑 |
| 敏感字段输出 | `SensitiveFieldResponse<T>`，被角色响应引用 | 访问器、组装逻辑、可见性判断 |
| Feat | `featIds`、`CharacterFeatResponse` | 实体、查询、校验和关联处理 |

关键未完成位置：

- [CharacterService.java](src/main/java/com/example/demo/Service/CharacterService.java) 第 14–18 行：`createPlayerCharacter()` 只查 Campaign，未使用 `request`，没有创建、保存或返回角色。
- `CharacterService` 缺少 `@Service`，目前没有其他代码将其注册为 Spring Bean。
- [CharacterCreateRequest.java](src/main/java/com/example/demo/DTO/CharacterCreateRequest.java) 第 12–53 行：字段较完整，但多数没有校验，也没有消费这些字段的业务逻辑。
- [CharacterResponse.java](src/main/java/com/example/demo/DTO/CharacterResponse.java) 第 10–43 行：包含 `maxHp`、角色类型、敏感字段、技能、物品、好感度等输出字段，但没有生成响应的代码。

## 三、明显缺失的功能

基于现有代码，继续完成角色创建所缺少的内容：

1. **角色及其关联数据的持久化层**：Character、Skill、CharacterSkill、CharacterItem、Affinity 均没有实体和 Repository。
2. **用户／房间成员系统的服务层和接口**：已有 `CampaignMember` 实体和 `DM / PLAYER` 枚举，但没有 User 实体、CampaignMember Repository、Service、Controller 或成员管理接口；`ownerMemberId` 仍只是角色请求 DTO 字段。
3. **Character 创建接口和完整 Service**：没有接收请求、校验关联对象、保存角色及子记录、返回 DTO 的完整链路。
4. **业务校验与数据库约束**：角色重名、成员归属、技能合法性等没有实现。
5. **现有模块的收尾**：Campaign 更新、删除、请求 DTO 与校验缺失；Campaign 单条查询没有明确的不存在时 404 处理。

`featIds` 已出现在输入中，后续需要明确并实现其处理，避免接收字段但实际忽略。

## 四、当前实体关系

当前已经声明的业务实体关系：

```text
Campaign 1:N Memo
Campaign 1:N CampaignMember
```

具体代码：[Memo.java](src/main/java/com/example/demo/Entity/Memo.java)，第 16–18 行。

```java
@ManyToOne
@JoinColumn(name = "campaign_id")
private Campaign campaign;
```

- Memo 持有 `campaign_id`，是单向关联。
- Campaign 没有 `List<Memo>` 或 `@OneToMany`。
- 没有声明级联操作、孤儿删除或非空关联约束。
- 没有显式设置懒加载；当前 `@ManyToOne` 使用默认 EAGER。

[CampaignMember.java](src/main/java/com/example/demo/Entity/CampaignMember.java) 第 20–21 行另有单向 `@ManyToOne Campaign`，但没有显式 `@JoinColumn`、非空约束或级联设置。其 `role` 字段拟以字符串形式保存 `CampaignMemberRole`（`DM / PLAYER`）；当前 `EnumType` 命名冲突导致这段映射无法编译。

以下关系尚未存在于 JPA 代码中：

- Campaign → Character
- Character → CharacterSkill → Skill
- Character → CharacterItem
- Character → NPC Affinity

DTO 中的 `campaignId`、`ownerMemberId`、`targetCharacterId` 只是字段，不能视为数据库外键。

### PostgreSQL / JPA 配置

[application.properties](src/main/resources/application.properties)：

- PostgreSQL：`localhost:5432/trpg_campaign_manager`。
- 用户名已配置，密码为空。
- `spring.jpa.hibernate.ddl-auto=update`。
- `spring.jpa.show-sql=true`。

[pom.xml](pom.xml) 使用 Spring Boot `4.1.1`、Java `21`，包含 JPA、PostgreSQL 驱动、Validation、Lombok、Web MVC、Actuator、DevTools 和测试依赖。

未发现数据库迁移脚本、独立测试数据源或 SQL 初始化文件。以上为代码配置，未核对数据库中的实际表结构。

## 五、Character 创建流程

当前实际流程：

```text
进入 createPlayerCharacter(campaignId, request)
→ 查询 Campaign
→ 不存在则抛 CampaignNotFoundException
→ 后续未实现，方法缺少返回值
```

下表的【已完成】仅表示该项逻辑已写出，不表示角色创建接口已能运行。代码路径均相对于项目根目录。

| 检查项 | 状态 | 实际实现及代码位置 |
| --- | --- | --- |
| Campaign 是否存在 | 【已完成】 | `Service/CharacterService.java:17` 使用 `findById().orElseThrow()` |
| ownerMember 是否存在 | 【未完成】 | `DTO/CharacterCreateRequest.java:42` 有 `ownerMemberId`，`Entity/CampaignMember.java` 有实体，但没有成员 Repository 或查询逻辑 |
| ownerMember 是否属于 Campaign | 【未完成】 | `Entity/CampaignMember.java:20–21` 有 Campaign 关联；Service 没有归属检查 |
| ownerMember role 是否为 PLAYER | 【未完成】 | `Entity/CampaignMemberRole.java:3–5` 有 PLAYER 枚举；Service 没有角色判断 |
| Character name 是否重复 | 【未完成】 | `DTO/CharacterCreateRequest.java:13` 仅有 `@NotBlank`；没有重名查询或唯一约束 |
| 六维属性是否完整 | 【未完成】 | `DTO/CharacterCreateRequest.java:24–29` 六个 Integer 字段存在，但都允许 null，没有范围校验 |
| gender / nationality / occupation / identity | 【未完成】 | `DTO/CharacterCreateRequest.java:16–19` 只有 String 字段，无校验 |
| level / rank / currentHp | 【未完成】 | `DTO/CharacterCreateRequest.java:21–22、31` 无非空、范围或相互约束；没有 HP 计算 |
| cashCents / debtCents / incomeCents / expenseCents | 【未完成】 | `DTO/CharacterCreateRequest.java:33–36` 使用 Long，但没有校验、默认值或保存 |
| desire / fear / secret | 【未完成】 | `DTO/CharacterCreateRequest.java:38–40` 只有输入字段，无保存和敏感输出处理 |
| skills 输入是否合法 | 【部分完成】 | `DTO/CharacterCreateRequest.java:44–45` 列表有 `@Valid`，输入 ID 有 `@NotNull`；没有接口触发、技能存在性、重复项、null 元素或业务范围检查 |
| CharacterSkill 的 skillId / value / unknown | 【部分完成】 | `DTO/CharacterSkillInput.java:5–11` 有字段雏形，但 ID 名为 `Id`，不是 `skillId`；没有值域、unknown 联动规则或保存 |
| items 是否正确处理 | 【部分完成】 | `DTO/CharacterItemInput.java:8–16` 已声明名称非空、数量非空且至少为 1；没有数据绑定配套访问器、业务处理和保存 |
| affinities 是否正确处理 | 【部分完成】 | `DTO/NpcAffinityInput.java:5–9` 目标 ID、intimacy 有 `@NotNull`；没有目标存在性、Campaign 归属、NPC 类型、数值范围或保存处理 |

表中 Java 路径的共同前缀为 `src/main/java/com/example/demo/`。

相关文件：

- [CharacterService.java](src/main/java/com/example/demo/Service/CharacterService.java)
- [CharacterCreateRequest.java](src/main/java/com/example/demo/DTO/CharacterCreateRequest.java)
- [CharacterSkillInput.java](src/main/java/com/example/demo/DTO/CharacterSkillInput.java)
- [CharacterItemInput.java](src/main/java/com/example/demo/DTO/CharacterItemInput.java)
- [NpcAffinityInput.java](src/main/java/com/example/demo/DTO/NpcAffinityInput.java)

Character 没有 Controller，也没有 Service 方法校验配置，因此 DTO 注解目前尚未形成可调用的校验链路。

## 六、当前可能存在的问题

### 1. 已确认的编译错误

原检查的离线编译明确失败：

```text
CharacterService.java:[18,5] 缺少返回语句
```

本次加入成员模型后的重新编译首先失败：

```text
CampaignMember.java:[25,25] 找不到符号 STRING
```

[CampaignMember.java](src/main/java/com/example/demo/Entity/CampaignMember.java) 第 14 行声明了名为 `EnumType` 的 String 字段，遮蔽了导入的 `jakarta.persistence.EnumType`，使第 25 行的 `EnumType.STRING` 无法解析。[CharacterService.java](src/main/java/com/example/demo/Service/CharacterService.java) 的方法仍没有 `return`，其原有编译错误尚未解决。

### 2. 两份 CampaignRepository，存在启动冲突风险

同时存在：

- [Repository/CampaignRepository.java](src/main/java/com/example/demo/Repository/CampaignRepository.java)
- [DTO/CampaignRepository.java](src/main/java/com/example/demo/DTO/CampaignRepository.java)

两者都继承 `JpaRepository<Campaign, Long>`，都处于应用扫描范围内。在当前默认命名下会竞争 `campaignRepository` Bean 名称，修复编译后仍需要处理。

三个 Service 和 `MemoServiceTest` 当前引用的都是 DTO 包中的 Repository。这不是泛型类型错误，但属于明确的分层和重复定义问题。

### 3. 部分 DTO 缺少可用的访问方式

以下类没有 getter/setter、Lombok 或显式 JSON 字段配置：

- `CharacterSkillInput`
- `CharacterItemInput`
- `NpcAffinityInput`
- `CharacterSkillResponse`
- `SensitiveFieldResponse`

其中部分字段为 private，部分为包级可见。从其他包中的 Service 读取、组装这些对象，以及按当前配置进行 JSON 输入输出，都存在问题，不能视为已完成 DTO。

`CharacterSkillInput.Id` 与响应中的 `skillId` 命名不一致。`unknown` 使用原始 `boolean`，无法区分客户端未提供与明确提供 false。

### 4. Validation 不完整

- Campaign 没有输入 DTO、字段约束或 Controller `@Valid`。
- Character 绝大多数字段没有约束。
- 列表上的 `@Valid` 不等于列表必填，也不保证元素非 null。
- 关联 ID 存在性、成员归属、角色重名等业务校验缺失。
- DTO 字符串长度没有与数据库列长度协调。

### 5. Exception Handling 覆盖不完整

[GlobalExceptionHandler.java](src/main/java/com/example/demo/Exception/GlobalExceptionHandler.java) 已处理两种 NotFound、无效请求体和 Bean Validation，但：

- 参数校验只返回第一个字段错误。
- 关联冲突、重复数据和数据库约束异常没有专门处理。
- Campaign 按 ID 查询直接返回 `Optional`，没有使用已有的 `CampaignNotFoundException`。
- 查询不存在的 Campaign 的 Memo 列表时，Service 不检查 Campaign，可能返回空列表。

该类导入了 Spring 的 `ErrorResponse`，同时声明同名内部 record。这里的返回和构造使用内部 record，本次编译没有报错；外部 import 多余且容易误导，不应误判成“实例化接口导致编译失败”。

### 6. JPA、数据库约束和事务

- 三个 Entity 除主键外，没有明确声明业务字段的非空、唯一等约束。
- `Memo.campaign` 没有 `optional=false` / `nullable=false`，映射允许无 Campaign 的 Memo。
- `MemoController.toResponse()` 直接调用 `memo.getCampaign().getId()`；如果数据库存在无关联记录，会出现空指针。
- 没有 Service 级事务。Repository 保存有其自身事务，但查询、修改、保存不是明确的整体事务。
- 角色创建未来需要一次保存多个关联对象，目前没有对应事务实现。
- 没有显式乐观锁版本字段，并发更新 Memo 可能互相覆盖。

### 7. Lazy Loading、DTO 混用和循环 JSON

- 当前没有显式 LAZY 关系，不能认定已经存在懒加载异常。
- Memo → Campaign、CampaignMember → Campaign 默认 EAGER，查询关联对象时可能额外加载 Campaign，具体 SQL 需要运行验证。
- Campaign API 直接使用 Entity，Memo API 使用 DTO，接口分层不一致。
- 当前只有单向关系，Memo 又转成 DTO 返回，未发现实际的循环 JSON 链路。

### 8. 测试覆盖不足

现有测试没有覆盖 Memo 更新、删除、查询异常、Controller Validation、JSON 映射或 JPA 约束。`contextLoads()` 没有独立测试数据库配置，运行时会依赖当前 PostgreSQL 配置。已有两个 Mockito 测试不能验证数据库映射或重复 Repository 的启动问题。

## 七、下一步建议

按依赖关系推进以下 5 步：

1. **恢复可编译、可启动的基线**：处理 CampaignMember 的 EnumType 命名冲突、CharacterService 未完成返回，合并 CampaignRepository 并统一导入；随后验证上下文启动和现有 Memo 测试。
2. **补齐 Character 创建所需的数据模型**：完善现有 CampaignMember 及其 Repository，再补 Character、Skill、CharacterSkill、CharacterItem、Affinity 的实体关系及必要约束。
3. **整理 Character DTO 并补校验**：统一 `skillId`，补访问器，明确必填字段、数值范围、unknown 规则和嵌套输入规则。
4. **实现事务性的 Character 创建链路**：按 Campaign → ownerMember → 归属和角色 → 重名及字段 → 关联输入的顺序校验，保存后组装响应，并接入 Controller。
5. **验证关键路径并收尾已有接口**：补角色创建失败／回滚及成功持久化测试，再完善 Campaign DTO、404 和更新／删除行为。

## 总体进度简表

| 模块 | 当前进度 |
| --- | --- |
| Campaign | 创建、查询已实现，CRUD 未完整 |
| Memo | 基础 CRUD 代码齐全，运行验证受构建阻塞 |
| Character Entity | 未开始 |
| Character 创建 | 起步，仅完成 Campaign 存在性检查 |
| Skill / CharacterSkill | 仅 DTO 雏形 |
| CharacterItem | DTO 与基础校验已写，持久化未开始 |
| CampaignMember / 用户系统 | 成员 Entity 和角色枚举已有雏形；成员业务层与用户实体未开始 |
| NPC Affinity | 仅 DTO 雏形 |
| 后端整体 | 当前不可编译，尚未达到角色创建可用阶段 |
