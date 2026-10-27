# TRPG 项目进展报告

更新日期：2026-10-27。依据当前工作区源码整理，包含 `demo/` 中尚未提交的改动。以下区分“已有实现”和“经过运行验证”；`demo/backend-progress-review.md` 是 9 月的历史检查，不能代表当前代码。

## 总体状态

项目由 Spring Boot 后端 `demo/` 和 React、TypeScript、Vite 前端 `frontend/` 组成。后端已有团务、成员、备忘录、玩家角色及 NPC 的模型和主要接口；前端已接入角色列表、详情、创建和编辑，另有尚未接入应用入口的备忘录页面。当前仍是功能原型：团务与成员 ID 写死，表单字段不完整，尚未完成端到端验证。

| 模块 | 已有实现 | 待完成 |
| --- | --- | --- |
| 团务 | 创建、列表、单条查询 | 更新、删除、输入校验、前端页面 |
| 成员 | 创建；DM／PLAYER 身份 | 列表、修改、移除、前端管理 |
| 备忘录 | 后端 CRUD；前端页面及组件 | 页面接入 `App`、动态团务选择 |
| 玩家角色 | 后端创建、列表、详情、更新、删除；前端演示流程 | 完整表单、测试及真实数据流程 |
| NPC | 后端创建与亲密度处理代码 | 创建流程修正与验证、前端界面 |
| 私密字段 | 后端按查看者身份隐藏值 | 前端完整展示与权限测试 |

## 后端：每个业务方法

### 团务与成员

| 方法 | 当前行为与状态 |
| --- | --- |
| `CampaignController.createCampaign` → `CampaignService.createCampaign` | `POST /api/campaigns`；直接接收并保存 `Campaign` 实体，没有请求 DTO 或字段校验。 |
| `CampaignController.getallCampaigns` → `CampaignService.getAllCampaigns` | `GET /api/campaigns`；使用 Repository `findAll` 返回全部团务。 |
| `CampaignController.getCampaignById` → `CampaignService.getCampaignById` | `GET /api/campaigns/{id}`；返回 `Optional`，不存在时缺少明确的 404 处理。 |
| `CampaignMemberController.createMember` → `CampaignMemberService.createMember` | `POST /api/campaigns/{campaignId}/members`；先检查团务存在，再保存成员显示名和 DM／PLAYER 身份。请求没有 `@Valid`；暂无成员查询、修改、删除方法。 |

源码：[CampaignController](demo/src/main/java/com/example/demo/controller/CampaignController.java)、[CampaignService](demo/src/main/java/com/example/demo/service/CampaignService.java)、[CampaignMemberController](demo/src/main/java/com/example/demo/controller/CampaignMemberController.java)、[CampaignMemberService](demo/src/main/java/com/example/demo/service/CampaignMemberService.java)。

### 备忘录

| 方法 | 当前行为与状态 |
| --- | --- |
| `MemoController.createMemo` → `MemoService.createMemo` | `POST /api/campaigns/{campaignId}/memos`；校验请求字段，检查团务存在，保存名称、分类和描述。 |
| `MemoController.getMemosByCampaignId` → `MemoService.getMemosByCampaignId` | `GET /api/campaigns/{campaignId}/memos`；查询后逐条转换为响应。服务未先检查团务是否存在。 |
| `MemoController.getMemoById` → `MemoService.getMemoById` | `GET /api/memos/{memoId}`；不存在时返回 404。 |
| `MemoController.updateMemo` → `MemoService.updateMemo` | `PUT /api/memos/{memoId}`；校验请求，更新名称、分类、描述。 |
| `MemoController.deleteMemo` → `MemoService.deleteMemo` | `DELETE /api/memos/{memoId}`；查找后删除，成功返回 204。 |
| `MemoController.toResponse` | 将实体转换成包含 ID、团务 ID、名称、分类、描述的响应；依赖 `memo.getCampaign()` 非空。 |

源码：[MemoController](demo/src/main/java/com/example/demo/controller/MemoController.java)、[MemoService](demo/src/main/java/com/example/demo/service/MemoService.java)。类别 `NPC`、`SECRET`、`LOCATION`、`ORGANIZATION`、`ITEM`、`EVENT`、`CUSTOM` 中的 `NPC` 只是备忘录分类。

### 角色公开方法

| 方法 | 当前行为与状态 |
| --- | --- |
| `CharacterController.createPlayerCharacter` → `CharacterService.createPlayerCharacter` | `POST /api/campaigns/{campaignId}/player-characters`；检查团务、必填字段、所属成员存在且属于该团务、成员为 PLAYER、团务内不重名；事务内保存角色、技能、物品、专长并返回响应。数值范围及数据库级唯一约束仍不完整。 |
| `CharacterController.createNpcCharacter` → `CharacterService.createNpcCharacter` | `POST /api/campaigns/{campaignId}/npcs`；检查团务、重名、NPC 无所属成员，并处理技能、专长、物品和亲密度目标。但服务在保存 NPC 本体前先保存技能关联，带技能创建存在持久化风险，未确认跑通；创建响应固定显示私密值。 |
| `CharacterController.getCharacter` → `CharacterService.getCharacterResponse` | `GET /api/characters/{characterId}?viewerMemberId=...`；检查角色与查看者存在、查看者属于同一团务，再加载关联数据并按权限返回。 |
| `CharacterController.getCharacters` → `CharacterService.getCharacters` | `GET /api/campaigns/{campaignId}/characters?viewerMemberId=...`；检查团务和查看者，逐个角色读取关联数据与组装响应；列表扩大时可能有较多数据库查询。 |
| `CharacterController.updateCharacter` → `CharacterService.updateCharacter` | `PUT /api/characters/{characterId}?viewerMemberId=...`；仅 DM 或所属成员可修改；检查改名冲突，更新基础和私密字段。请求未加 `@Valid`，也不更新技能、物品、专长、亲密度。 |
| `CharacterController.deleteCharacter` → `CharacterService.deleteCharacter` | `DELETE /api/characters/{characterId}?viewerMemberId=...`；仅 DM 或所属成员可删除；先清理技能、物品及作为来源／目标的亲密度，再删除角色。控制器未显式设置 204。 |

源码：[CharacterController](demo/src/main/java/com/example/demo/controller/CharacterController.java)、[CharacterService](demo/src/main/java/com/example/demo/service/CharacterService.java)。`viewerMemberId` 来自请求参数，目前没有登录身份或认证流程。

### 角色服务内部方法

| 方法 | 作用 |
| --- | --- |
| `validatePlayerCharacterRequest` | 检查所属成员、基础字段、财务、背景信息非空，要求至少一项技能；拒绝未知技能和 NPC 亲密度。多数数值缺少范围约束。 |
| `validateNpcCharacterRequest` | 拒绝 NPC 携带所属成员 ID。 |
| `fillBasicFields` | 将姓名、身份、等级、六维、当前 HP、财务与背景字段写入实体。 |
| `buildCharacterSkills` | 拒绝重复技能类型，查询技能定义，构造角色技能关联。 |
| `buildCharacterItems` | 将物品输入转成角色物品实体；空输入返回空列表。 |
| `resolveFeats` | 拒绝重复专长 ID，并检查专长是否都存在。 |
| `calculateMaxHp` | 按 `体质 × 2 + 等级 × 5` 计算；体质或等级缺失时返回 `null`。 |
| `canViewPrivateFields` | DM 或角色所属成员可查看私密值。 |

### 映射、仓库与基础设施

[CharacterMapper](demo/src/main/java/com/example/demo/mapper/CharacterMapper.java) 的 `toSkillResponse`、`toItemResponse`、`toFeatResponse`、`toAffinityResponse` 分别转换四类关联数据；`toResponse` 汇总角色基础信息、HP、财务、背景及关联列表；`sensitive` 将私密字段包装为 `{visible, value}`，不可见时把 `value` 设为 `null`。

[Repository 目录](demo/src/main/java/com/example/demo/Repository/) 的自定义方法包括：`MemoRepository.findByCampaignId`；`CharacterRepository.existsByCampaign_IdAndName`、`findByCampaign_Id`；`CharacterSkillRepository` 和 `CharacterItemRepository` 各自的 `findByCharacter_Id`、`deleteByCharacter_Id`；`NpcAffinityRepository.findByNpcCharacter_Id`、`deleteByNpcCharacter_Id`、`deleteByTargetCharacter_Id`；`SkillRepository.findAllByTypeIn`、`findByType`；`FeatRepository.findAllById`。其余基础 CRUD 来自 JPA Repository。

[SkillDataInitializer.initSkills](demo/src/main/java/com/example/demo/config/SkillDataInitializer.java) 在启动时补齐技能类型；[WebConfig.addCorsMappings](demo/src/main/java/com/example/demo/config/WebConfig.java) 允许本地 Vite 站点访问 API。[GlobalExceptionHandler](demo/src/main/java/com/example/demo/exception/GlobalExceptionHandler.java) 中 `handleMemoNotFound`、`handleCampaignNotFound`、`handleCharacterNotFound`、`handleCampaignMemberNotFound` 返回 404；`handleUnreadableMessage`、`handleValidationError`、`handleBusinessRule` 返回 400。校验错误目前只返回第一个字段错误。

## 前端：每个页面、组件与方法

| 文件／组件 | 已实现的方法与交互 | 当前限制 |
| --- | --- | --- |
| [App.tsx](frontend/src/App.tsx) | `App` 维护选中角色和列表刷新键，组合角色列表、详情和创建表单。 | 团务 ID 固定 `1`，查看者／所属成员 ID 固定 `2`；备忘录页面未接入。 |
| [CharacterList.tsx](frontend/src/components/CharacterList.tsx) | `useEffect` 调用 `getCharacters`，点击角色名时回调选中；创建后可刷新。 | 无加载、错误、空状态与删除入口。 |
| [CharacterDetail.tsx](frontend/src/components/CharacterDetail.tsx) | `loadCharacter` 获取详情；组件展示名称、HP、现金、欲望、秘密，挂载编辑表单；更新后重新加载。 | 只展示部分字段；Hook 依赖有 lint 警告。 |
| [CreateCharacterForm.tsx](frontend/src/components/CreateCharacterForm.tsx) | `handleSubmit` 直接 `fetch` 创建玩家角色；管理提交状态、错误，成功后刷新列表。 | 用户只填姓名，其余属性与技能使用固定示例值；无 NPC 表单。 |
| [EditCharacterForm.tsx](frontend/src/components/EditCharacterForm.tsx) | `handleSubmit` 直接 `fetch` 更新角色；可编辑姓名、当前 HP、现金、欲望。 | 其余字段从详情回填；不可见私密字段的 `null` 可能再次提交并覆盖原值。 |
| [MemoPage.tsx](frontend/src/pages/MemoPage.tsx) | `loadMemos` 加载列表，`handleSave` 创建／更新，`handleEdit` 选择编辑项，`handleDelete` 删除；支持类别筛选。 | 未被 `App` 引用；团务 ID 固定 `1`；错误状态替代整个页面。 |
| [MemoCard.tsx](frontend/src/components/MemoCard.tsx) | 展示名称与描述；编辑、删除按钮调用父组件回调。 | 两个未使用导入阻塞 TypeScript 构建。 |
| [MemoForm.tsx](frontend/src/components/MemoForm.tsx) | `useEffect` 填入待编辑内容；`handleSubmit` 提交名称、类别、描述并清空输入。 | 编辑时按钮仍写“创建Memo”；effect 中设置状态有 lint 警告。 |

[characterApi.ts](frontend/src/api/characterApi.ts) 的 `getCharacter`、`getCharacters` 分别获取单个和团务内角色；[memoApi.ts](frontend/src/api/memoApi.ts) 的 `getMemos`、`createMemo`、`updateMemo`、`deleteMemo` 对应备忘录列表、创建、更新和删除。角色创建／更新尚未统一放入 API 模块。[character.ts](frontend/src/types/character.ts) 和 [memo.ts](frontend/src/types/memo.ts) 定义前端数据类型。

## 本次验证

| 命令 | 结果 |
| --- | --- |
| `cd demo && mvn -o -q -DskipTests clean compile` | 成功，后端当前可编译。 |
| `cd demo && mvn -o test` | 5 项测试均报环境错误：4 项 Mockito 测试因当前 JDK 的 Byte Buddy 代理无法自行附加而未执行断言；应用上下文测试因本地 PostgreSQL 不可用而失败。不能据此判断业务断言通过或失败。 |
| `cd frontend && npm run build` | 失败：`MemoCard.tsx` 两个未使用导入触发 TypeScript 错误。 |
| `cd frontend && npm run lint` | 命令完成，报告 5 条警告：两个未使用导入，另有 `MemoForm`、`CharacterDetail` 的 effect／依赖警告。 |

现有测试：[MemoServiceTest](demo/src/test/java/com/example/demo/MemoServiceTest.java) 覆盖两个创建场景；[CharacterServiceTest](demo/src/test/java/com/example/demo/CharacterServiceTest.java) 覆盖所属成员查看私密字段和跨团查看；[DemoApplicationTests](demo/src/test/java/com/example/demo/DemoApplicationTests.java) 检查应用上下文。尚缺角色创建、NPC 亲密度、更新、删除、HTTP 接口与数据库持久化测试。

## 下一步优先级

1. 清理 `MemoCard` 未使用导入，恢复前端构建；修正编辑表单对不可见私密字段的提交方式。
2. 调整 NPC 创建时角色与技能关联的保存顺序，并用数据库验证完整创建流程。
3. 提供可用的测试数据库，解决 Mockito 代理环境问题，重新运行测试。
4. 接入动态团务和成员上下文，补齐角色表单及 NPC 页面，把备忘录页面接入应用入口。
5. 补团务／成员管理、角色更新校验和关键权限测试。
