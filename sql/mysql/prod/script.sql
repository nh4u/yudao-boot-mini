create table ems_additional_recording
(
    id                bigint auto_increment comment 'id'
        primary key,
    voucher_id        bigint                                                           null comment '凭证id',
    standingbook_id   bigint                                                           null comment '计量器具id',
    value_type        int(1)                                                           null comment '增量/全量',
    this_collect_time datetime                                                         null comment '本次采集时间',
    this_value        decimal(30, 10)                                                  null comment '本次数值',
    pre_collect_time  datetime                                                         null comment '上次采集时间',
    pre_value         decimal(30, 10)                                                  null comment '上次采集值',
    unit              varchar(255)                                                     null comment '单位',
    record_person     varchar(255)                                                     null comment '补录人',
    record_reason     varchar(255)                                                     null comment '补录原因',
    record_method     int(1)                                                           null comment '补录方式',
    enter_time        datetime                                                         null comment '录入时间',
    creator           varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time       datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater           varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time       datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted           bit                                    default b'0'              not null comment '是否删除',
    tenant_id         bigint                                 default 0                 not null comment '租户编号'
)
    comment '补录表' row_format = DYNAMIC;

create table ems_coal_factor_history
(
    id          bigint auto_increment comment 'id'
        primary key,
    energy_id   bigint                                                           null comment '能源id',
    factor      decimal(10, 6)                                                   null comment '折标煤系数',
    formula_id  bigint                                                           null comment '公式id',
    formula     varchar(255)                                                     null comment '关联计算公式',
    creator     varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit                                    default b'0'              not null comment '是否删除',
    tenant_id   bigint                                 default 0                 not null comment '租户编号',
    start_time  datetime                                                         null comment '生效开始时间',
    end_time    datetime                                                         null comment '生效结束时间'
)
    comment '折标煤系数历史表' row_format = DYNAMIC;

create index idx_energy_id
    on ems_coal_factor_history (energy_id);

create index idx_formula_id
    on ems_coal_factor_history (formula_id);

create table ems_da_param_formula
(
    id                   bigint auto_increment comment 'id'
        primary key,
    energy_id            bigint                                                           not null comment '能源id',
    formula_status       int                                                              not null comment '公式状态【0:未使用；1：使用中；2：已使用】',
    energy_formula       varchar(255)                                                     null comment '能源参数计算公式',
    formula_type         int                                                              null comment '公式类型[1折标煤公式;2用能成本公式]',
    formula_scale        int                                    default 5                 null comment '公式小数点',
    start_effective_time datetime                                                         null comment '开始生效时间',
    end_effective_time   datetime                                                         null comment '结束生效时间',
    creator              varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time          datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater              varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time          datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted              bit                                    default b'0'              not null comment '是否删除',
    tenant_id            bigint                                 default 0                 not null comment '租户编号'
)
    comment '数据来源为关联计量器具时的参数公式表' row_format = DYNAMIC;

create table ems_energy_configuration
(
    id                   bigint auto_increment comment 'id'
        primary key,
    group_id             bigint                                                           null comment '分組id',
    energy_name          varchar(255)                                                     null comment '能源名称',
    code                 varchar(255)                                                     null comment '编码',
    energy_classify      int(1)                                                           null comment '能源分类',
    energy_icon          json                                                             null comment '能源图标',
    factor               decimal(10, 6)                                                   null comment '折标煤系数',
    coal_formula         varchar(255)                                                     null comment '折标煤公式',
    coal_scale           varchar(255)                                                     null comment '折标煤小数位数',
    start_time           datetime                                                         null comment '开始时间',
    end_time             datetime                                                         null comment '结束时间',
    billing_method       int(1)                                                           null comment '计费方式  |  1：统一计价  2：分时段计价  3：阶梯计价',
    accounting_frequency int(1)                                                           null comment '核算频率  |  1：按月   2：按季   3：按年',
    unit_price           json                                                             null comment '单价详细',
    unit_price_formula   varchar(255)                                                     null comment '单价公式',
    unit_price_scale     varchar(255)                                                     null comment '单价小数位',
    creator              varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time          datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater              varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time          datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted              bit                                    default b'0'              not null comment '是否删除',
    tenant_id            bigint                                 default 0                 not null comment '租户编号'
)
    comment '能源配置表' row_format = DYNAMIC;

create table ems_energy_group
(
    id          bigint auto_increment comment '编号'
        primary key,
    name        varchar(255) collate utf8mb4_unicode_ci                          not null comment '分组名称',
    sort        int(4)                                 default 1                 null comment '排序',
    creator     varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit                                    default b'0'              not null comment '是否删除',
    tenant_id   bigint                                 default 0                 not null comment '租户编号'
)
    comment '能源分组表' row_format = DYNAMIC;

create table ems_energy_parameters
(
    id           bigint auto_increment comment 'id'
        primary key,
    energy_id    bigint                                                           not null comment '能源id',
    parameter    varchar(255)                                                     null comment '参数名称',
    code         varchar(255)                                                     null comment '编码',
    data_feature int(1)                                                           null comment '数据特征',
    unit         varchar(255)                                                     null comment '单位',
    data_type    int(1)                                                           null comment '数据类型',
    `usage`      int(1)                                                           null comment '用量',
    creator      varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time  datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater      varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time  datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted      bit                                    default b'0'              not null comment '是否删除',
    tenant_id    bigint                                 default 0                 not null comment '租户编号'
)
    comment '能源参数表' row_format = DYNAMIC;

create index idx_energy_id
    on ems_energy_parameters (energy_id);

create table ems_header_code_mapping
(
    id          bigint auto_increment comment '编号'
        primary key,
    header_code varchar(50) collate utf8mb4_unicode_ci                           not null comment '表头code',
    header      varchar(255)                           default '1'               null comment '表头',
    code        varchar(50) collate utf8mb4_unicode_ci                           not null comment '系统台账code',
    type        int(4)                                 default 1                 not null comment '类型0：去空串完全匹配；1：去空串首部匹配；2：去空串尾部匹配；5：去尾部-完全匹配；6：去尾部-首部匹配；7：去尾部-尾部匹配；8：未匹配到。',
    creator     varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit                                    default b'0'              not null comment '是否删除',
    tenant_id   bigint                                 default 0                 not null comment '租户编号'
)
    row_format = DYNAMIC;

create table ems_label_config
(
    id          bigint auto_increment comment '编号'
        primary key,
    label_name  varchar(255) collate utf8mb4_unicode_ci                           not null comment '标签名称',
    sort        int(4)                                  default 1                 not null comment '排序',
    remark      varchar(255) collate utf8mb4_unicode_ci default ''                null comment '备注',
    code        varchar(100) collate utf8mb4_unicode_ci default ''                null comment '编码',
    if_default  varchar(1)                                                        null comment '是否为默认标签',
    parent_id   bigint                                  default 0                 not null comment '父标签ID',
    creator     varchar(64) collate utf8mb4_unicode_ci  default ''                null comment '创建者',
    create_time datetime                                default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) collate utf8mb4_unicode_ci  default ''                null comment '更新者',
    update_time datetime                                default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit                                     default b'0'              not null comment '是否删除',
    tenant_id   bigint                                  default 0                 not null comment '租户编号'
)
    comment '配置标签' row_format = DYNAMIC;

create table ems_price_detail
(
    id           bigint auto_increment comment 'id'
        primary key,
    price_id     bigint                                                           null comment '单价id',
    period_type  int(1)                                                           null comment '时段类型',
    period_start time                                                             null comment '时段开始时间',
    period_end   time                                                             null comment '时段结束时间',
    usage_min    decimal(30, 10)                                                  null comment '档位用量下限',
    usage_max    decimal(30, 10)                                                  null comment '档位用量上限',
    unit_price   decimal(30, 10)                                                  null comment '单价',
    creator      varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time  datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater      varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time  datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted      bit                                    default b'0'              not null comment '是否删除',
    tenant_id    bigint                                 default 0                 not null comment '租户编号'
)
    comment '单价详细表（计费详细）' row_format = DYNAMIC;

create table ems_unit_price_configuration
(
    id                   bigint auto_increment comment 'id'
        primary key,
    energy_id            bigint                                                           null comment '能源id',
    start_time           datetime                                                         null comment '开始时间',
    end_time             datetime                                                         null comment '结束时间',
    billing_method       int(1)                                                           null comment '计费方式  |  1：统一计价  2：分时段计价  3：阶梯计价',
    accounting_frequency int(1)                                                           null comment '核算频率  |  1：按月   2：按季   3：按年',
    formula_id           bigint                                                           null comment '公式id',
    formula              varchar(255)                                                     null comment '计算公式',
    creator              varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time          datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater              varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time          datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted              bit                                    default b'0'              not null comment '是否删除',
    tenant_id            bigint                                 default 0                 not null comment '租户编号'
)
    comment '单价配置表（单价周期）' row_format = DYNAMIC;

create index idx_energy_time
    on ems_unit_price_configuration (energy_id, start_time, end_time);

create table ems_voucher
(
    id            bigint auto_increment comment '编号'
        primary key,
    code          varchar(100) collate utf8mb4_unicode_ci default ''                not null comment '凭证编号',
    name          varchar(255) collate utf8mb4_unicode_ci                           not null comment '凭证名称',
    energy_id     bigint                                                            not null comment '能源id',
    energy_name   varchar(255)                                                      null comment '能源name',
    purchase_time datetime                                default CURRENT_TIMESTAMP null comment '购入时间',
    attention     varchar(255) collate utf8mb4_unicode_ci                           null comment '经办人',
    price         decimal(30, 10)                         default 0.0000000000      null comment '金额',
    `usage`       decimal(30, 10)                         default 0.0000000000      null comment '用量',
    usage_unit    varchar(100)                                                      null comment '用量单位',
    description   varchar(1000) collate utf8mb4_unicode_ci                          null comment '描述',
    appendix_name varchar(255) collate utf8mb4_unicode_ci                           null comment '附件名称',
    appendix_url  varchar(255) collate utf8mb4_unicode_ci                           null comment '附件地址',
    results       text collate utf8mb4_unicode_ci                                   null comment '识别结果',
    appendix      json                                                              null comment '凭证附件',
    creator       varchar(64) collate utf8mb4_unicode_ci  default ''                null comment '创建者',
    create_time   datetime                                default CURRENT_TIMESTAMP not null comment '创建时间',
    updater       varchar(64) collate utf8mb4_unicode_ci  default ''                null comment '更新者',
    update_time   datetime                                default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted       bit                                     default b'0'              not null comment '是否删除',
    tenant_id     bigint                                  default 0                 not null comment '租户编号',
    month         date                                                              null comment '月份'
)
    comment '凭证管理表' row_format = DYNAMIC;

create table infra_api_access_log
(
    id               bigint auto_increment comment '日志主键'
        primary key,
    trace_id         varchar(64)  default ''                not null comment '链路追踪编号',
    user_id          bigint       default 0                 not null comment '用户编号',
    user_type        tinyint      default 0                 not null comment '用户类型',
    application_name varchar(50)                            not null comment '应用名',
    request_method   varchar(16)  default ''                not null comment '请求方法名',
    request_url      varchar(255) default ''                not null comment '请求地址',
    request_params   text                                   null comment '请求参数',
    response_body    text                                   null comment '响应结果',
    user_ip          varchar(50)                            not null comment '用户 IP',
    user_agent       varchar(512)                           not null comment '浏览器 UA',
    operate_module   varchar(50)                            null comment '操作模块',
    operate_name     varchar(50)                            null comment '操作名',
    operate_type     tinyint      default 0                 null comment '操作分类',
    begin_time       datetime                               not null comment '开始请求时间',
    end_time         datetime                               not null comment '结束请求时间',
    duration         int                                    not null comment '执行时长',
    result_code      int          default 0                 not null comment '结果码',
    result_msg       varchar(512) default ''                null comment '结果提示',
    creator          varchar(64)  default ''                null comment '创建者',
    create_time      datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater          varchar(64)  default ''                null comment '更新者',
    update_time      datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted          bit          default b'0'              not null comment '是否删除',
    tenant_id        bigint       default 0                 not null comment '租户编号'
)
    comment 'API 访问日志表' collate = utf8mb4_unicode_ci
                             row_format = DYNAMIC;

create index idx_create_time
    on infra_api_access_log (create_time);

create table infra_api_error_log
(
    id                           bigint auto_increment comment '编号'
        primary key,
    trace_id                     varchar(64)                            not null comment '链路追踪编号',
    user_id                      int          default 0                 not null comment '用户编号',
    user_type                    tinyint      default 0                 not null comment '用户类型',
    application_name             varchar(50)                            not null comment '应用名',
    request_method               varchar(16)                            not null comment '请求方法名',
    request_url                  varchar(255)                           not null comment '请求地址',
    request_params               varchar(8000)                          not null comment '请求参数',
    user_ip                      varchar(50)                            not null comment '用户 IP',
    user_agent                   varchar(512)                           not null comment '浏览器 UA',
    exception_time               datetime                               not null comment '异常发生时间',
    exception_name               varchar(128) default ''                not null comment '异常名',
    exception_message            text                                   not null comment '异常导致的消息',
    exception_root_cause_message text                                   not null comment '异常导致的根消息',
    exception_stack_trace        text                                   not null comment '异常的栈轨迹',
    exception_class_name         varchar(512)                           not null comment '异常发生的类全名',
    exception_file_name          varchar(512)                           not null comment '异常发生的类文件',
    exception_method_name        varchar(512)                           not null comment '异常发生的方法名',
    exception_line_number        int                                    not null comment '异常发生的方法所在行',
    process_status               tinyint                                not null comment '处理状态',
    process_time                 datetime                               null comment '处理时间',
    process_user_id              int          default 0                 null comment '处理用户编号',
    creator                      varchar(64)  default ''                null comment '创建者',
    create_time                  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater                      varchar(64)  default ''                null comment '更新者',
    update_time                  datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted                      bit          default b'0'              not null comment '是否删除',
    tenant_id                    bigint       default 0                 not null comment '租户编号'
)
    comment '系统异常日志' collate = utf8mb4_unicode_ci
                           row_format = DYNAMIC;

create table infra_codegen_column
(
    id                       bigint auto_increment comment '编号'
        primary key,
    table_id                 bigint                                 not null comment '表编号',
    column_name              varchar(200)                           not null comment '字段名',
    data_type                varchar(100)                           not null comment '字段类型',
    column_comment           varchar(500)                           not null comment '字段描述',
    nullable                 bit                                    not null comment '是否允许为空',
    primary_key              bit                                    not null comment '是否主键',
    ordinal_position         int                                    not null comment '排序',
    java_type                varchar(32)                            not null comment 'Java 属性类型',
    java_field               varchar(64)                            not null comment 'Java 属性名',
    dict_type                varchar(200) default ''                null comment '字典类型',
    example                  varchar(64)                            null comment '数据示例',
    create_operation         bit                                    not null comment '是否为 Create 创建操作的字段',
    update_operation         bit                                    not null comment '是否为 Update 更新操作的字段',
    list_operation           bit                                    not null comment '是否为 List 查询操作的字段',
    list_operation_condition varchar(32)  default '='               not null comment 'List 查询操作的条件类型',
    list_operation_result    bit                                    not null comment '是否为 List 查询操作的返回字段',
    html_type                varchar(32)                            not null comment '显示类型',
    creator                  varchar(64)  default ''                null comment '创建者',
    create_time              datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater                  varchar(64)  default ''                null comment '更新者',
    update_time              datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted                  bit          default b'0'              not null comment '是否删除'
)
    comment '代码生成表字段定义' collate = utf8mb4_unicode_ci
                                 row_format = DYNAMIC;

create table infra_codegen_table
(
    id                    bigint auto_increment comment '编号'
        primary key,
    data_source_config_id bigint                                 not null comment '数据源配置的编号',
    scene                 tinyint      default 1                 not null comment '生成场景',
    table_name            varchar(200) default ''                not null comment '表名称',
    table_comment         varchar(500) default ''                not null comment '表描述',
    remark                varchar(500)                           null comment '备注',
    module_name           varchar(30)                            not null comment '模块名',
    business_name         varchar(30)                            not null comment '业务名',
    class_name            varchar(100) default ''                not null comment '类名称',
    class_comment         varchar(50)                            not null comment '类描述',
    author                varchar(50)                            not null comment '作者',
    template_type         tinyint      default 1                 not null comment '模板类型',
    front_type            tinyint                                not null comment '前端类型',
    parent_menu_id        bigint                                 null comment '父菜单编号',
    master_table_id       bigint                                 null comment '主表的编号',
    sub_join_column_id    bigint                                 null comment '子表关联主表的字段编号',
    sub_join_many         bit                                    null comment '主表与子表是否一对多',
    tree_parent_column_id bigint                                 null comment '树表的父字段编号',
    tree_name_column_id   bigint                                 null comment '树表的名字字段编号',
    creator               varchar(64)  default ''                null comment '创建者',
    create_time           datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater               varchar(64)  default ''                null comment '更新者',
    update_time           datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted               bit          default b'0'              not null comment '是否删除'
)
    comment '代码生成表定义' collate = utf8mb4_unicode_ci
                             row_format = DYNAMIC;

create table infra_config
(
    id          bigint auto_increment comment '参数主键'
        primary key,
    category    varchar(50)                            not null comment '参数分组',
    type        tinyint                                not null comment '参数类型',
    name        varchar(100) default ''                not null comment '参数名称',
    config_key  varchar(100) default ''                not null comment '参数键名',
    value       varchar(500) default ''                not null comment '参数键值',
    visible     bit                                    not null comment '是否可见',
    remark      varchar(500)                           null comment '备注',
    creator     varchar(64)  default ''                null comment '创建者',
    create_time datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64)  default ''                null comment '更新者',
    update_time datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit          default b'0'              not null comment '是否删除'
)
    comment '参数配置表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table infra_data_source_config
(
    id          bigint auto_increment comment '主键编号'
        primary key,
    name        varchar(100) default ''                not null comment '参数名称',
    url         varchar(1024)                          not null comment '数据源连接',
    username    varchar(255)                           not null comment '用户名',
    password    varchar(255) default ''                not null comment '密码',
    creator     varchar(64)  default ''                null comment '创建者',
    create_time datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64)  default ''                null comment '更新者',
    update_time datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit          default b'0'              not null comment '是否删除'
)
    comment '数据源配置表' collate = utf8mb4_unicode_ci
                           row_format = DYNAMIC;

create table infra_file
(
    id          bigint auto_increment comment '文件编号'
        primary key,
    config_id   bigint                                null comment '配置编号',
    name        varchar(256)                          null comment '文件名',
    path        varchar(512)                          not null comment '文件路径',
    url         varchar(1024)                         not null comment '文件 URL',
    type        varchar(128)                          null comment '文件类型',
    size        int                                   not null comment '文件大小',
    creator     varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit         default b'0'              not null comment '是否删除'
)
    comment '文件表' collate = utf8mb4_unicode_ci
                     row_format = DYNAMIC;

create table infra_file_config
(
    id          bigint auto_increment comment '编号'
        primary key,
    name        varchar(63)                           not null comment '配置名',
    storage     tinyint                               not null comment '存储器',
    remark      varchar(255)                          null comment '备注',
    master      bit                                   not null comment '是否为主配置',
    config      varchar(4096)                         not null comment '存储配置',
    creator     varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit         default b'0'              not null comment '是否删除'
)
    comment '文件配置表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table infra_file_content
(
    id          bigint auto_increment comment '编号'
        primary key,
    config_id   bigint                                not null comment '配置编号',
    path        varchar(512)                          not null comment '文件路径',
    content     mediumblob                            not null comment '文件内容',
    creator     varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit         default b'0'              not null comment '是否删除'
)
    comment '文件表' collate = utf8mb4_unicode_ci
                     row_format = DYNAMIC;

create table infra_job
(
    id              bigint auto_increment comment '任务编号'
        primary key,
    name            varchar(32)                           not null comment '任务名称',
    status          tinyint                               not null comment '任务状态',
    handler_name    varchar(64)                           not null comment '处理器的名字',
    handler_param   varchar(255)                          null comment '处理器的参数',
    cron_expression varchar(32)                           not null comment 'CRON 表达式',
    retry_count     int         default 0                 not null comment '重试次数',
    retry_interval  int         default 0                 not null comment '重试间隔',
    monitor_timeout int         default 0                 not null comment '监控超时时间',
    creator         varchar(64) default ''                null comment '创建者',
    create_time     datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater         varchar(64) default ''                null comment '更新者',
    update_time     datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         bit         default b'0'              not null comment '是否删除'
)
    comment '定时任务表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table infra_job_log
(
    id            bigint auto_increment comment '日志编号'
        primary key,
    job_id        bigint                                  not null comment '任务编号',
    handler_name  varchar(64)                             not null comment '处理器的名字',
    handler_param varchar(255)                            null comment '处理器的参数',
    execute_index tinyint       default 1                 not null comment '第几次执行',
    begin_time    datetime                                not null comment '开始执行时间',
    end_time      datetime                                null comment '结束执行时间',
    duration      int                                     null comment '执行时长',
    status        tinyint                                 not null comment '任务状态',
    result        varchar(4000) default ''                null comment '结果数据',
    creator       varchar(64)   default ''                null comment '创建者',
    create_time   datetime      default CURRENT_TIMESTAMP not null comment '创建时间',
    updater       varchar(64)   default ''                null comment '更新者',
    update_time   datetime      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted       bit           default b'0'              not null comment '是否删除'
)
    comment '定时任务日志表' collate = utf8mb4_unicode_ci
                             row_format = DYNAMIC;

create table power_chemicals_settings
(
    id          bigint auto_increment comment '编号'
        primary key,
    code        varchar(40) collate utf8mb4_unicode_ci                           not null comment '类型',
    time        datetime                                                         not null comment '日期',
    price       decimal(15, 2)                                                   null comment '金额',
    creator     varchar(40) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(40) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit                                    default b'0'              not null comment '是否删除',
    tenant_id   bigint                                 default 0                 not null comment '租户编号'
)
    comment '化学品数据设置表';

create table power_cop_formula
(
    id          bigint auto_increment comment '编号'
        primary key,
    cop_type    varchar(40)                            not null comment '低温冷机 LTC,低温系统 LTS,中温冷机 MTC,中温系统 MTS',
    formula     varchar(512)                           not null comment '公式',
    creator     varchar(255) default ''                null comment '创建者',
    create_time datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(255) default ''                null comment '更新者',
    update_time datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit          default b'0'              not null comment '是否删除',
    tenant_id   bigint       default 0                 not null comment '租户编号'
)
    comment 'cop报表公式' collate = utf8mb4_unicode_ci
                          row_format = DYNAMIC;

create index idx_cop_type
    on power_cop_formula (cop_type);

create table power_cop_settings
(
    id              bigint auto_increment comment '编号'
        primary key,
    cop_type        varchar(40)                            not null comment '低温冷机 LTC,低温系统 LTS,中温冷机 MTC,中温系统 MTS',
    data_feature    int(4)                                 not null comment '数据特征 1累计值2稳态值3状态值',
    param           varchar(40)                            not null comment '公式参数',
    param_cn_name   varchar(100)                           not null comment '公式参数对应能源参数中文名',
    standingbook_id bigint                                 null comment '台账id',
    creator         varchar(255) default ''                null comment '创建者',
    create_time     datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater         varchar(255) default ''                null comment '更新者',
    update_time     datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         bit          default b'0'              not null comment '是否删除',
    tenant_id       bigint       default 0                 not null comment '租户编号'
)
    comment 'cop报表公式参数配置' collate = utf8mb4_unicode_ci
                                  row_format = DYNAMIC;

create index idx_standingbook_id
    on power_cop_settings (standingbook_id);

create table power_device_monitor_qrcode
(
    id          bigint auto_increment comment '编号'
        primary key,
    device_id   bigint                                                           not null comment '用户id',
    qrcode      varchar(300)                                                     not null comment '二维码内容',
    creator     varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit                                    default b'0'              not null comment '是否删除',
    tenant_id   bigint                                 default 0                 not null comment '租户编号'
)
    comment '设备监控-设备二维码维护' row_format = DYNAMIC;

create table power_double_carbon_mapping
(
    id                 bigint auto_increment comment '编号'
        primary key,
    standingbook_id    bigint                                                           not null comment '台账id',
    standingbook_code  varchar(400)                                                     not null comment '台账编码',
    double_carbon_code varchar(400)                                                     null comment '双碳编码',
    creator            varchar(40) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time        datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater            varchar(40) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time        datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted            bit                                    default b'0'              not null comment '是否删除',
    tenant_id          bigint                                 default 0                 not null comment '租户编号'
)
    comment '双碳对接 映射';

create table power_double_carbon_settings
(
    id                    bigint auto_increment comment '编号'
        primary key,
    name                  varchar(255) collate utf8mb4_unicode_ci                          not null comment '系统名称',
    url                   varchar(255) collate utf8mb4_unicode_ci                          not null comment '接口地址',
    update_frequency      int                                                              not null comment '更新频率',
    update_frequency_unit tinyint                                                          not null comment '更新频率单位',
    last_sync_time        datetime                                                         null comment '上次同步时间',
    creator               varchar(40) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time           datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater               varchar(40) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time           datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted               bit                                    default b'0'              not null comment '是否删除',
    tenant_id             bigint                                 default 0                 not null comment '租户编号'
)
    comment '双碳对接设置';

create table power_external_api
(
    id          bigint auto_increment comment '编号'
        primary key,
    name        varchar(100) collate utf8mb4_unicode_ci                          not null comment '接口名称',
    code        varchar(100) collate utf8mb4_unicode_ci                          not null comment '接口编码',
    url         varchar(255) collate utf8mb4_unicode_ci                          not null comment '接口地址',
    method      varchar(40) collate utf8mb4_unicode_ci                           not null comment '请求方式',
    body        text                                                             null comment 'body',
    creator     varchar(40) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(40) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit                                    default b'0'              not null comment '是否删除',
    tenant_id   bigint                                 default 0                 not null comment '租户编号'
)
    comment '外部接口表';

create table power_gas_measurement
(
    id               bigint                             not null comment '主键'
        primary key,
    measurement_name varchar(255)                       null comment '计量器具名称（可变，不作为依据）',
    measurement_code varchar(255)                       not null comment '计量器具编号（即台账code）',
    sort_no          int(6)   default 0                 not null comment '排序',
    creator          varchar(40)                        null comment '创建者',
    create_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '创建时间',
    updater          varchar(40)                        null comment '更新者',
    update_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted          bit      default b'0'              not null comment '是否删除',
    tenant_id        bigint   default 1                 not null comment '租户编号',
    energy_param     varchar(40)                        not null comment '能源参数中文名'
)
    row_format = DYNAMIC;

create table power_measurement_association
(
    id                        bigint auto_increment comment 'id'
        primary key,
    measurement_instrument_id bigint                                                           null comment '计量器具id',
    measurement_id            bigint                                                           null comment '关联下级计量',
    creator                   varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time               datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater                   varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time               datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted                   bit                                    default b'0'              not null comment '是否删除',
    tenant_id                 bigint                                 default 0                 not null comment '租户编号'
)
    comment '计量器具下级计量配置表' row_format = DYNAMIC;

create index idx_deleted
    on power_measurement_association (deleted);

create index idx_measurement_instrument_id
    on power_measurement_association (measurement_instrument_id);

create table power_measurement_device
(
    id                        bigint auto_increment comment 'id'
        primary key,
    measurement_instrument_id bigint                                                           null comment '计量器具id',
    device_id                 bigint                                                           null comment '关联设备',
    creator                   varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time               datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater                   varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time               datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted                   bit                                    default b'0'              not null comment '是否删除',
    tenant_id                 bigint                                 default 0                 not null comment '租户编号'
)
    comment '计量器具上级设备配置表' row_format = DYNAMIC;

create index idx_deleted
    on power_measurement_device (deleted);

create index idx_device_id
    on power_measurement_device (device_id);

create index idx_measurement_instrument_id
    on power_measurement_device (measurement_instrument_id);

create table power_measurement_virtual_association
(
    id                        bigint auto_increment comment 'id'
        primary key,
    measurement_instrument_id bigint                                                           null comment '计量器具id(虚拟表)',
    measurement_id            bigint                                                           null comment '关联下级计量',
    creator                   varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time               datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater                   varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time               datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted                   bit                                    default b'0'              not null comment '是否删除',
    tenant_id                 bigint                                 default 0                 not null comment '租户编号'
)
    comment '虚拟表的计量器具下级计量配置表' row_format = DYNAMIC;

create index idx_deleted
    on power_measurement_virtual_association (deleted);

create index idx_measurement_instrument_id
    on power_measurement_virtual_association (measurement_instrument_id);

create table power_month_plan_settings
(
    id          bigint auto_increment comment '编号'
        primary key,
    energy_name varchar(255) collate utf8mb4_unicode_ci                          not null comment '能源名称',
    energy_code varchar(255)                                                     not null comment '能源编号',
    energy_unit varchar(40)                                                      null comment '能源单位',
    plan        decimal(22, 2)                                                   null comment '计划用量',
    creator     varchar(40) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(40) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit                                    default b'0'              not null comment '是否删除',
    tenant_id   bigint                                 default 0                 not null comment '租户编号'
)
    comment '本月计划设置表';

create table power_production
(
    id          bigint auto_increment comment '编号'
        primary key,
    time        datetime                                                         not null comment '获取时间',
    origin_time varchar(40)                                                      null comment '原始时间',
    plan        decimal(15, 2)                                                   null comment '计划产量',
    lot         decimal(15, 2)                                                   null comment '实际产量',
    size        int                                                              not null comment '产量尺寸',
    value       decimal(15, 2)                                                   null comment '间隔产量数',
    creator     varchar(40) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(40) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit                                    default b'0'              not null comment '是否删除',
    tenant_id   bigint                                 default 0                 not null comment '租户编号'
);

create table power_production_consumption_settings
(
    id              bigint auto_increment comment '编号'
        primary key,
    name            varchar(255) collate utf8mb4_unicode_ci                          null comment '统计项名称',
    standingbook_id bigint                                                           null comment '台账id',
    creator         varchar(40) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time     datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater         varchar(40) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time     datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         bit                                    default b'0'              not null comment '是否删除',
    tenant_id       bigint                                 default 0                 not null comment '租户编号'
)
    row_format = DYNAMIC;

create table power_pure_waste_water_gas_settings
(
    id               bigint auto_increment comment '编号'
        primary key,
    `system`         varchar(40) collate utf8mb4_unicode_ci                           null comment '类型',
    code             varchar(40) collate utf8mb4_unicode_ci                           null comment '编码',
    name             varchar(40) collate utf8mb4_unicode_ci                           not null comment '名称',
    energy_codes     varchar(255)                                                     null comment '能源codes',
    standingbook_ids text                                                             null comment '台账ids',
    creator          varchar(40) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time      datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater          varchar(40) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time      datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted          bit                                    default b'0'              not null comment '是否删除',
    tenant_id        bigint                                 default 0                 not null comment '租户编号'
)
    comment '纯废水压缩空气设置表';

create table power_service_settings
(
    id           bigint auto_increment comment 'id'
        primary key,
    service_name varchar(255)                                                     not null comment '服务名称',
    protocol     tinyint                                default 0                 null comment '协议类型(0：OPCDA 1:MODBUS-TCP)',
    ip_address   varchar(15)                                                      not null comment 'IP地址',
    port         int                                                              not null comment 'SMTP 服务器端口',
    retry_count  int                                    default 3                 not null comment '重试次数，默认3',
    clsid        varchar(255)                                                     null comment '注册表ID',
    username     varchar(255)                                                     null comment '用户名',
    password     varchar(255)                                                     not null comment '密码',
    creator      varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time  datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater      varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time  datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted      bit                                    default b'0'              not null comment '是否删除',
    tenant_id    bigint                                 default 0                 not null comment '租户编号'
)
    comment '服务设置表' row_format = DYNAMIC;

create table power_share_file_settings
(
    id          bigint auto_increment comment '编号'
        primary key,
    name        varchar(100) collate utf8mb4_unicode_ci                          not null comment '部门名称',
    type        int                                                              not null comment '目录拼接类型[1：年月日；2：年。]',
    ip          varchar(100) collate utf8mb4_unicode_ci                          not null comment '部门服务器ip地址',
    dir         varchar(255) collate utf8mb4_unicode_ci                          not null comment '共享文件夹地址前缀',
    creator     varchar(40) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(40) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit                                    default b'0'              not null comment '是否删除',
    tenant_id   bigint                                 default 0                 not null comment '租户编号'
)
    comment '内网共享文件设置表';

create table power_standingbook
(
    id                           bigint auto_increment comment '编号'
        primary key,
    name                         varchar(255) default ''                null comment '属性名字',
    stage                        int(1)                                 null comment '环节 | 1：外购存储  2：加工转换 3：传输分配 4：终端使用 5：回收利用',
    description                  varchar(255)                           null comment '简介',
    creator                      varchar(255) default ''                null comment '创建者',
    create_time                  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater                      varchar(255) default ''                null comment '更新者',
    update_time                  datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted                      bit          default b'0'              not null comment '是否删除',
    tenant_id                    bigint       default 0                 not null comment '租户编号',
    type_id                      bigint                                 null comment '类型id',
    frequency                    bigint                                 null comment '采集频率',
    frequency_Unit               varchar(255)                           null comment '采集频率单位',
    source_Type                  bigint                                 null comment '数据来源分类',
    association_Measurement_Json text                                   null comment '数据来源分类=关联计量器具时 相关信息json',
    status                       bit                                    null comment '开关（0：关；1开。）'
)
    comment '台账表' collate = utf8mb4_unicode_ci
                     row_format = DYNAMIC;

create table power_standingbook_acquisition
(
    id                  bigint auto_increment comment '编号'
        primary key,
    standingbook_id     bigint                                 not null comment '台账id',
    status              bit                                    not null comment '设备数采启停开关（0：关；1开。）',
    frequency           bigint                                 not null comment '采集频率',
    frequency_unit      int(4)                                 not null comment '采集频率单位(秒、分钟、小时、天)',
    service_settings_id bigint                                 not null comment '服务设置id',
    start_time          datetime                               not null comment '开始时间',
    creator             varchar(255) default ''                null comment '创建者',
    create_time         datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater             varchar(255) default ''                null comment '更新者',
    update_time         datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted             bit          default b'0'              not null comment '是否删除',
    tenant_id           bigint       default 0                 not null comment '租户编号'
)
    comment '台账-数采设置表' collate = utf8mb4_unicode_ci
                              row_format = DYNAMIC;

create index idx_standingbook_id
    on power_standingbook_acquisition (standingbook_id);

create table power_standingbook_acquisition_detail
(
    id                   bigint auto_increment comment '编号'
        primary key,
    acquisition_id       bigint                                 not null comment '数采设置id',
    status               bit                                    not null comment '参数采集开关（0：关；1开。）',
    data_site            varchar(255)                           null comment 'OPCDA：io地址/MODBUS：',
    formula              varchar(255)                           null comment '第一层公式',
    actual_formula       varchar(255)                           null comment '实际公式到io级别',
    full_increment       int(4)                                 null comment '全量/增量（0：全量；1增量。）',
    code                 varchar(255)                           not null comment '参数编码',
    energy_flag          bit                                    not null comment '参数类型（能源数采1/自定义数采0）',
    creator              varchar(255) default ''                null comment '创建者',
    create_time          datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater              varchar(255) default ''                null comment '更新者',
    update_time          datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted              bit          default b'0'              not null comment '是否删除',
    tenant_id            bigint       default 0                 not null comment '租户编号',
    modbus_salve         varchar(255)                           null comment '从地址',
    modbus_register_type varchar(40)                            null comment '寄存器地址'
)
    comment '台账-数采设置-详细信息表' collate = utf8mb4_unicode_ci
                                       row_format = DYNAMIC;

create table power_standingbook_attribute
(
    id              bigint auto_increment comment '编号'
        primary key,
    standingbook_id bigint                                 null comment '台账编号',
    type_id         bigint                                 null comment '台账类型id',
    name            varchar(255) default ''                not null comment '属性名',
    value           text                                   null comment '属性值',
    file_id         bigint                                 null comment '文件编号',
    is_required     varchar(255)                           not null comment '是否必填',
    code            varchar(255)                           not null comment '编码',
    sort            bigint                                 null comment '排序',
    format          varchar(255)                           null comment '格式',
    node            varchar(255)                           null comment '归属节点(无用待删)',
    options         varchar(255)                           null comment '下拉框选项',
    description     varchar(255)                           null comment '简介',
    creator         varchar(255) default ''                null comment '创建者',
    create_time     datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater         varchar(255) default ''                null comment '更新者',
    update_time     datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         bit          default b'0'              not null comment '是否删除',
    tenant_id       bigint       default 0                 not null comment '租户编号',
    auto_generated  varchar(255) default '1'               null comment '是否系统生成',
    raw_attr_id     bigint                                 null comment '源属性id',
    node_id         bigint                                 null comment '归属节点(台账分类id)',
    display_flag    varchar(255) default '1'               null comment '是否展示'
)
    comment '台账属性表（+分类属性表）' collate = utf8mb4_unicode_ci
                                      row_format = DYNAMIC;

create table power_standingbook_label_info
(
    id              bigint auto_increment comment '编号'
        primary key,
    standingbook_id bigint                                 null comment '台账id',
    name            varchar(255) default ''                not null comment '标签key',
    value           varchar(255)                           null comment '标签值',
    creator         varchar(255) default ''                null comment '创建者',
    create_time     datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater         varchar(255) default ''                null comment '更新者',
    update_time     datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         bit          default b'0'              not null comment '是否删除',
    tenant_id       bigint       default 0                 not null comment '租户编号'
)
    comment '台账标签表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create index idx_standingbook_id
    on power_standingbook_label_info (standingbook_id);

create table power_standingbook_tmpl_daq_attr
(
    id             bigint auto_increment comment '分类数采参数属性id'
        primary key,
    type_id        bigint                                 null comment '台账分类id',
    energy_id      bigint                                 null comment '所属能源id',
    energy_flag    bit          default b'0'              not null comment '是否能源数采参数 0自定义 1能源参数',
    parameter_id   bigint                                 null comment '能源参数id',
    parameter      varchar(255)                           null comment '参数名称',
    code           varchar(255)                           null comment '编码',
    data_feature   int(1)                                 null comment '数据特征',
    unit           varchar(255)                           null comment '单位',
    data_type      int(1)                                 null comment '数据类型',
    `usage`        int(1)                                 null comment '用量',
    raw_attr_id    bigint                                 null comment '数采源属性id',
    node_id        bigint                                 null comment '源属性归属节点(台账分类id)',
    auto_generated varchar(255) default '1'               null comment '是否自定义属性，0是系统继承属性，1是模板私有属性',
    status         bit          default b'1'              not null comment '是否启用',
    creator        varchar(255) default ''                null comment '创建者',
    create_time    datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater        varchar(255) default ''                null comment '更新者',
    update_time    datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted        bit          default b'0'              not null comment '是否删除',
    tenant_id      bigint       default 0                 not null comment '租户编号',
    sort           int(4)                                 null comment '排序'
)
    comment '台账分类的数采参数表（自定义和能源）' collate = utf8mb4_unicode_ci
                                                 row_format = DYNAMIC;

create table power_standingbook_type
(
    id          bigint auto_increment comment '编号'
        primary key,
    name        varchar(255) default ''                not null comment '名字',
    super_id    bigint                                 null comment '父级类型编号',
    super_name  varchar(255)                           null comment '父级名字',
    top_type    varchar(255)                           not null comment '类型',
    sort        bigint                                 null comment '排序',
    level       bigint                                 null comment '当前层级(无用待删)',
    code        varchar(255)                           null comment '编码',
    description varchar(255)                           null comment '简介',
    creator     varchar(255) default ''                null comment '创建者',
    create_time datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(255) default ''                null comment '更新者',
    update_time datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit          default b'0'              not null comment '是否删除',
    tenant_id   bigint       default 0                 not null comment '租户编号',
    is_default  varchar(255) default '0'               null comment '是否是系统默认属性，1是 0不是'
)
    comment '台账类型表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table power_supply_analysis_settings
(
    id              bigint auto_increment comment '编号'
        primary key,
    `system`        varchar(40) collate utf8mb4_unicode_ci                           not null comment '系统',
    item            varchar(40)                                                      not null comment '分析项',
    standingbook_id bigint                                                           null comment '台账id',
    creator         varchar(40) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time     datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater         varchar(40) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time     datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         bit                                    default b'0'              not null comment '是否删除',
    tenant_id       bigint                                 default 0                 not null comment '租户编号'
)
    comment '供应分设置析表' row_format = DYNAMIC;

create table power_supply_water_tmp_settings
(
    id                bigint auto_increment comment '编号'
        primary key,
    code              varchar(40) collate utf8mb4_unicode_ci                           null comment '标识',
    `system`          varchar(40) collate utf8mb4_unicode_ci                           not null comment '系统',
    standingbook_id   bigint                                                           null comment '台账id',
    energy_param_name varchar(40) collate utf8mb4_unicode_ci                           null comment '能源参数名称',
    energy_param_code varchar(40) collate utf8mb4_unicode_ci                           null comment '能源参数编码（数据表用）',
    max               decimal(5, 2)                                                    null comment '上限',
    min               decimal(5, 2)                                                    null comment '下限',
    creator           varchar(40) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time       datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater           varchar(40) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time       datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted           bit                                    default b'0'              not null comment '是否删除',
    tenant_id         bigint                                 default 0                 not null comment '租户编号'
)
    comment '供水温度报表' row_format = DYNAMIC;

create table power_tank_settings
(
    id                   bigint auto_increment comment '编号'
        primary key,
    name                 varchar(40) collate utf8mb4_unicode_ci                           not null comment '储罐名称',
    standingbook_id      bigint                                                           null comment '台账id',
    density              decimal(8, 5)                                                    null comment '密度ρ',
    gravity_acceleration decimal(8, 5)                                                    null comment '重力加速度g',
    creator              varchar(40) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time          datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater              varchar(40) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time          datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted              bit                                    default b'0'              not null comment '是否删除',
    tenant_id            bigint                                 default 0                 not null comment '租户编号',
    pressure_diff_id     bigint                                                           null comment '设备压差id',
    sort_no              int(6)                                 default 0                 not null comment '排序',
    code                 varchar(255)                                                     not null comment '计量器具编码'
)
    comment '储罐液位设置表' row_format = DYNAMIC;

create index idx_tank_query
    on power_tank_settings (deleted, tenant_id, name);

create table power_transformer_utilization_settings
(
    id              bigint auto_increment comment '编号'
        primary key,
    transformer_id  bigint                                                           not null comment '变压器',
    load_current_id bigint                                                           null comment '负载电流',
    voltage_level   varchar(40)                                                      null comment '电压等级',
    rated_capacity  decimal(10, 2)                                                   null comment '额定容量',
    creator         varchar(40) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time     datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater         varchar(40) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time     datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         bit                                    default b'0'              not null comment '是否删除',
    tenant_id       bigint                                 default 0                 not null comment '租户编号'
)
    comment '变压器利用率设置' row_format = DYNAMIC;

create table power_warning_info
(
    id             bigint auto_increment comment '编号'
        primary key,
    level          tinyint                                                          not null comment '告警等级：紧急4 重要3 次要2 警告1 提示0',
    warning_time   datetime                                                         not null comment '告警时间',
    status         tinyint                                default 0                 not null comment '处理状态:0-未处理1-处理中2-已处理',
    device_rel     text                                                             not null comment '设备名称与编号',
    template_id    bigint                                                           not null comment '模板id',
    strategy_id    bigint                                                           null comment '策略id',
    title          text                                                             not null comment '标题',
    content        text                                                             null comment '内容（头部）',
    creator        varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time    datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater        varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time    datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted        bit                                    default b'0'              not null comment '是否删除',
    tenant_id      bigint                                 default 0                 not null comment '租户编号',
    handle_opinion varchar(500)                                                     null comment '处理意见'
)
    comment '告警信息表' row_format = DYNAMIC;

create table power_warning_info_user
(
    id          bigint auto_increment comment '编号'
        primary key,
    user_id     bigint                                                           not null comment '用户id',
    info_id     bigint                                                           not null comment '站内信id',
    creator     varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit                                    default b'0'              not null comment '是否删除',
    tenant_id   bigint                                 default 0                 not null comment '租户编号'
)
    comment '告警信息-用户关联表' row_format = DYNAMIC;

create table power_warning_strategy
(
    id                bigint auto_increment comment '编号'
        primary key,
    name              varchar(30)                                                      not null comment '规则名称',
    description       varchar(255)                                                     not null comment '描述',
    device_scope      text                                                             null comment '设备范围',
    device_type_scope text                                                             null comment '设备分类范围',
    level             tinyint                                                          not null comment '告警等级：紧急4 重要3 次要2 警告1 提示0',
    site_template_id  bigint                                                           not null comment '站内信模板id',
    mail_template_id  bigint                                                           null comment '邮件模板id',
    site_staff        text                                                             null comment '站内信人员',
    mail_staff        text                                                             null comment '邮件人员',
    common_staff      text                                                             null comment '公共人员通知',
    `interval`        int                                                              not null comment '告警间隔',
    interval_unit     tinyint                                                          not null comment '告警间隔单位',
    status            tinyint                                default 0                 not null comment '开启状态',
    creator           varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time       datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater           varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time       datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted           bit                                    default b'0'              not null comment '是否删除',
    tenant_id         bigint                                 default 0                 not null comment '租户编号',
    last_exp_time     datetime                                                         null comment '最新触发时间'
)
    comment '告警策略表' row_format = DYNAMIC;

create table power_warning_strategy_condition
(
    id          bigint auto_increment comment '编号'
        primary key,
    strategy_id bigint                                                           not null comment '策略id',
    param_id    text                                                             not null comment '条件参数-属性id，层级id+能源参数编码',
    connector   tinyint                                                          not null comment '条件连接符',
    value       varchar(255)                                                     not null comment '条件值',
    device_flag bit                                                              null comment '是否设备',
    creator     varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit                                    default b'0'              not null comment '是否删除',
    tenant_id   bigint                                 default 0                 not null comment '租户编号'
)
    comment '告警策略表条件' row_format = DYNAMIC;

create table power_warning_template
(
    id          bigint auto_increment comment '编号'
        primary key,
    name        varchar(255)                                                     not null comment '模板名称',
    code        varchar(255)                                                     not null comment '模板编码',
    content     text                                                             not null comment '模板内容',
    title       text                                                             not null comment '模板标题',
    t_params    text                                                             not null comment '标题参数数组',
    params      text                                                             not null comment '内容参数数组',
    remark      varchar(255)                                                     null comment '备注',
    type        tinyint                                                          not null comment '模板类型:0-站内信1-邮件',
    creator     varchar(64) collate utf8mb4_unicode_ci default ''                null comment '创建者',
    create_time datetime                               default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) collate utf8mb4_unicode_ci default ''                null comment '更新者',
    update_time datetime                               default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit                                    default b'0'              not null comment '是否删除',
    tenant_id   bigint                                 default 0                 not null comment '租户编号'
)
    comment '告警模板表' row_format = DYNAMIC;

create table system_dept
(
    id             bigint auto_increment comment '部门id'
        primary key,
    name           varchar(30) default ''                not null comment '部门名称',
    parent_id      bigint      default 0                 not null comment '父部门id',
    sort           int         default 0                 not null comment '显示顺序',
    leader_user_id bigint                                null comment '负责人',
    phone          varchar(11)                           null comment '联系电话',
    email          varchar(50)                           null comment '邮箱',
    status         tinyint                               not null comment '部门状态（0正常 1停用）',
    creator        varchar(64) default ''                null comment '创建者',
    create_time    datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater        varchar(64) default ''                null comment '更新者',
    update_time    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted        bit         default b'0'              not null comment '是否删除',
    tenant_id      bigint      default 0                 not null comment '租户编号'
)
    comment '部门表' collate = utf8mb4_unicode_ci
                     row_format = DYNAMIC;

create table system_dict_data
(
    id          bigint auto_increment comment '字典编码'
        primary key,
    sort        int          default 0                 not null comment '字典排序',
    label       varchar(100) default ''                not null comment '字典标签',
    value       varchar(100) default ''                not null comment '字典键值',
    dict_type   varchar(100) default ''                not null comment '字典类型',
    status      tinyint      default 0                 not null comment '状态（0正常 1停用）',
    color_type  varchar(100) default ''                null comment '颜色类型',
    css_class   varchar(100) default ''                null comment 'css 样式',
    remark      varchar(500)                           null comment '备注',
    creator     varchar(64)  default ''                null comment '创建者',
    create_time datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64)  default ''                null comment '更新者',
    update_time datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit          default b'0'              not null comment '是否删除'
)
    comment '字典数据表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table system_dict_type
(
    id           bigint auto_increment comment '字典主键'
        primary key,
    name         varchar(100) default ''                not null comment '字典名称',
    type         varchar(100) default ''                not null comment '字典类型',
    status       tinyint      default 0                 not null comment '状态（0正常 1停用）',
    remark       varchar(500)                           null comment '备注',
    creator      varchar(64)  default ''                null comment '创建者',
    create_time  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater      varchar(64)  default ''                null comment '更新者',
    update_time  datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted      bit          default b'0'              not null comment '是否删除',
    deleted_time datetime                               null comment '删除时间'
)
    comment '字典类型表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table system_login_log
(
    id          bigint auto_increment comment '访问ID'
        primary key,
    log_type    bigint                                not null comment '日志类型',
    trace_id    varchar(64) default ''                not null comment '链路追踪编号',
    user_id     bigint      default 0                 not null comment '用户编号',
    user_type   tinyint     default 0                 not null comment '用户类型',
    username    varchar(50) default ''                not null comment '用户账号',
    result      tinyint                               not null comment '登陆结果',
    user_ip     varchar(50)                           not null comment '用户 IP',
    user_agent  varchar(512)                          not null comment '浏览器 UA',
    creator     varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit         default b'0'              not null comment '是否删除',
    tenant_id   bigint      default 0                 not null comment '租户编号'
)
    comment '系统访问记录' collate = utf8mb4_unicode_ci
                           row_format = DYNAMIC;

create table system_mail_account
(
    id              bigint auto_increment comment '主键'
        primary key,
    mail            varchar(255)                          not null comment '邮箱',
    username        varchar(255)                          not null comment '用户名',
    password        varchar(255)                          not null comment '密码',
    host            varchar(255)                          not null comment 'SMTP 服务器域名',
    port            int                                   not null comment 'SMTP 服务器端口',
    ssl_enable      bit         default b'0'              not null comment '是否开启 SSL',
    starttls_enable bit         default b'0'              not null comment '是否开启 STARTTLS',
    creator         varchar(64) default ''                null comment '创建者',
    create_time     datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater         varchar(64) default ''                null comment '更新者',
    update_time     datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         bit         default b'0'              not null comment '是否删除'
)
    comment '邮箱账号表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table system_mail_log
(
    id                bigint auto_increment comment '编号'
        primary key,
    user_id           bigint                                null comment '用户编号',
    user_type         tinyint                               null comment '用户类型',
    to_mail           varchar(255)                          not null comment '接收邮箱地址',
    account_id        bigint                                not null comment '邮箱账号编号',
    from_mail         varchar(255)                          not null comment '发送邮箱地址',
    template_id       bigint                                not null comment '模板编号',
    template_code     varchar(63)                           not null comment '模板编码',
    template_nickname varchar(255)                          null comment '模版发送人名称',
    template_title    varchar(255)                          not null comment '邮件标题',
    template_content  varchar(10240)                        not null comment '邮件内容',
    template_params   varchar(255)                          null comment '邮件参数',
    send_status       tinyint     default 0                 not null comment '发送状态',
    send_time         datetime                              null comment '发送时间',
    send_message_id   varchar(255)                          null comment '发送返回的消息 ID',
    send_exception    varchar(4096)                         null comment '发送异常',
    creator           varchar(64) default ''                null comment '创建者',
    create_time       datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater           varchar(64) default ''                null comment '更新者',
    update_time       datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted           bit         default b'0'              not null comment '是否删除'
)
    comment '邮件日志表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table system_mail_template
(
    id          bigint auto_increment comment '编号'
        primary key,
    name        varchar(63)                           not null comment '模板名称',
    code        varchar(63)                           not null comment '模板编码',
    account_id  bigint                                not null comment '发送的邮箱账号编号',
    nickname    varchar(255)                          null comment '发送人名称',
    title       varchar(255)                          not null comment '模板标题',
    content     varchar(10240)                        not null comment '模板内容',
    params      varchar(255)                          not null comment '参数数组',
    status      tinyint                               not null comment '开启状态',
    remark      varchar(255)                          null comment '备注',
    creator     varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit         default b'0'              not null comment '是否删除'
)
    comment '邮件模版表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table system_menu
(
    id             bigint auto_increment comment '菜单ID'
        primary key,
    name           varchar(50)                            not null comment '菜单名称',
    permission     varchar(100) default ''                not null comment '权限标识',
    type           tinyint                                not null comment '菜单类型',
    sort           int          default 0                 not null comment '显示顺序',
    parent_id      bigint       default 0                 not null comment '父菜单ID',
    path           varchar(200) default ''                null comment '路由地址',
    icon           varchar(100) default '#'               null comment '菜单图标',
    component      varchar(255)                           null comment '组件路径',
    component_name varchar(255)                           null comment '组件名',
    status         tinyint      default 0                 not null comment '菜单状态',
    visible        bit          default b'1'              not null comment '是否可见',
    keep_alive     bit          default b'1'              not null comment '是否缓存',
    always_show    bit          default b'1'              not null comment '是否总是显示',
    creator        varchar(64)  default ''                null comment '创建者',
    create_time    datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater        varchar(64)  default ''                null comment '更新者',
    update_time    datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted        bit          default b'0'              not null comment '是否删除'
)
    comment '菜单权限表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table system_notice
(
    id          bigint auto_increment comment '公告ID'
        primary key,
    title       varchar(50)                           not null comment '公告标题',
    content     text                                  not null comment '公告内容',
    type        tinyint                               not null comment '公告类型（1通知 2公告）',
    status      tinyint     default 0                 not null comment '公告状态（0正常 1关闭）',
    creator     varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit         default b'0'              not null comment '是否删除',
    tenant_id   bigint      default 0                 not null comment '租户编号'
)
    comment '通知公告表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table system_notify_message
(
    id                bigint auto_increment comment '用户ID'
        primary key,
    user_id           bigint                                not null comment '用户id',
    user_type         tinyint                               not null comment '用户类型',
    template_id       bigint                                not null comment '模版编号',
    template_code     varchar(64)                           not null comment '模板编码',
    template_nickname varchar(63)                           not null comment '模版发送人名称',
    template_content  varchar(1024)                         not null comment '模版内容',
    template_type     int                                   not null comment '模版类型',
    template_params   varchar(255)                          not null comment '模版参数',
    read_status       bit                                   not null comment '是否已读',
    read_time         datetime                              null comment '阅读时间',
    creator           varchar(64) default ''                null comment '创建者',
    create_time       datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater           varchar(64) default ''                null comment '更新者',
    update_time       datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted           bit         default b'0'              not null comment '是否删除',
    tenant_id         bigint      default 0                 not null comment '租户编号'
)
    comment '站内信消息表' collate = utf8mb4_unicode_ci
                           row_format = DYNAMIC;

create table system_notify_template
(
    id          bigint auto_increment comment '主键'
        primary key,
    name        varchar(63)                           not null comment '模板名称',
    code        varchar(64)                           not null comment '模版编码',
    nickname    varchar(255)                          not null comment '发送人名称',
    content     varchar(1024)                         not null comment '模版内容',
    type        tinyint                               not null comment '类型',
    params      varchar(255)                          null comment '参数数组',
    status      tinyint                               not null comment '状态',
    remark      varchar(255)                          null comment '备注',
    creator     varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit         default b'0'              not null comment '是否删除'
)
    comment '站内信模板表' collate = utf8mb4_unicode_ci
                           row_format = DYNAMIC;

create table system_oauth2_access_token
(
    id            bigint auto_increment comment '编号'
        primary key,
    user_id       bigint                                not null comment '用户编号',
    user_type     tinyint                               not null comment '用户类型',
    user_info     varchar(512)                          not null comment '用户信息',
    access_token  varchar(255)                          not null comment '访问令牌',
    refresh_token varchar(32)                           not null comment '刷新令牌',
    client_id     varchar(255)                          not null comment '客户端编号',
    scopes        varchar(255)                          null comment '授权范围',
    expires_time  datetime                              not null comment '过期时间',
    creator       varchar(64) default ''                null comment '创建者',
    create_time   datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater       varchar(64) default ''                null comment '更新者',
    update_time   datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted       bit         default b'0'              not null comment '是否删除',
    tenant_id     bigint      default 0                 not null comment '租户编号'
)
    comment 'OAuth2 访问令牌' collate = utf8mb4_unicode_ci
                              row_format = DYNAMIC;

create index idx_access_token
    on system_oauth2_access_token (access_token);

create index idx_refresh_token
    on system_oauth2_access_token (refresh_token);

create table system_oauth2_approve
(
    id           bigint auto_increment comment '编号'
        primary key,
    user_id      bigint                                 not null comment '用户编号',
    user_type    tinyint                                not null comment '用户类型',
    client_id    varchar(255)                           not null comment '客户端编号',
    scope        varchar(255) default ''                not null comment '授权范围',
    approved     bit          default b'0'              not null comment '是否接受',
    expires_time datetime                               not null comment '过期时间',
    creator      varchar(64)  default ''                null comment '创建者',
    create_time  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater      varchar(64)  default ''                null comment '更新者',
    update_time  datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted      bit          default b'0'              not null comment '是否删除',
    tenant_id    bigint       default 0                 not null comment '租户编号'
)
    comment 'OAuth2 批准表' collate = utf8mb4_unicode_ci
                            row_format = DYNAMIC;

create table system_oauth2_client
(
    id                             bigint auto_increment comment '编号'
        primary key,
    client_id                      varchar(255)                          not null comment '客户端编号',
    secret                         varchar(255)                          not null comment '客户端密钥',
    name                           varchar(255)                          not null comment '应用名',
    logo                           varchar(255)                          not null comment '应用图标',
    description                    varchar(255)                          null comment '应用描述',
    status                         tinyint                               not null comment '状态',
    access_token_validity_seconds  int                                   not null comment '访问令牌的有效期',
    refresh_token_validity_seconds int                                   not null comment '刷新令牌的有效期',
    redirect_uris                  varchar(255)                          not null comment '可重定向的 URI 地址',
    authorized_grant_types         varchar(255)                          not null comment '授权类型',
    scopes                         varchar(255)                          null comment '授权范围',
    auto_approve_scopes            varchar(255)                          null comment '自动通过的授权范围',
    authorities                    varchar(255)                          null comment '权限',
    resource_ids                   varchar(255)                          null comment '资源',
    additional_information         varchar(4096)                         null comment '附加信息',
    creator                        varchar(64) default ''                null comment '创建者',
    create_time                    datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater                        varchar(64) default ''                null comment '更新者',
    update_time                    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted                        bit         default b'0'              not null comment '是否删除'
)
    comment 'OAuth2 客户端表' collate = utf8mb4_unicode_ci
                              row_format = DYNAMIC;

create table system_oauth2_code
(
    id           bigint auto_increment comment '编号'
        primary key,
    user_id      bigint                                 not null comment '用户编号',
    user_type    tinyint                                not null comment '用户类型',
    code         varchar(32)                            not null comment '授权码',
    client_id    varchar(255)                           not null comment '客户端编号',
    scopes       varchar(255) default ''                null comment '授权范围',
    expires_time datetime                               not null comment '过期时间',
    redirect_uri varchar(255)                           null comment '可重定向的 URI 地址',
    state        varchar(255) default ''                not null comment '状态',
    creator      varchar(64)  default ''                null comment '创建者',
    create_time  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater      varchar(64)  default ''                null comment '更新者',
    update_time  datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted      bit          default b'0'              not null comment '是否删除',
    tenant_id    bigint       default 0                 not null comment '租户编号'
)
    comment 'OAuth2 授权码表' collate = utf8mb4_unicode_ci
                              row_format = DYNAMIC;

create table system_oauth2_refresh_token
(
    id            bigint auto_increment comment '编号'
        primary key,
    user_id       bigint                                not null comment '用户编号',
    refresh_token varchar(32)                           not null comment '刷新令牌',
    user_type     tinyint                               not null comment '用户类型',
    client_id     varchar(255)                          not null comment '客户端编号',
    scopes        varchar(255)                          null comment '授权范围',
    expires_time  datetime                              not null comment '过期时间',
    creator       varchar(64) default ''                null comment '创建者',
    create_time   datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater       varchar(64) default ''                null comment '更新者',
    update_time   datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted       bit         default b'0'              not null comment '是否删除',
    tenant_id     bigint      default 0                 not null comment '租户编号'
)
    comment 'OAuth2 刷新令牌' collate = utf8mb4_unicode_ci
                              row_format = DYNAMIC;

create table system_operate_log
(
    id             bigint auto_increment comment '日志主键'
        primary key,
    trace_id       varchar(64)   default ''                not null comment '链路追踪编号',
    user_id        bigint                                  not null comment '用户编号',
    user_type      tinyint       default 0                 not null comment '用户类型',
    type           varchar(50)                             not null comment '操作模块类型',
    sub_type       varchar(50)                             not null comment '操作名',
    biz_id         bigint                                  not null comment '操作数据模块编号',
    action         varchar(2000) default ''                not null comment '操作内容',
    extra          varchar(2000) default ''                not null comment '拓展字段',
    request_method varchar(16)   default ''                null comment '请求方法名',
    request_url    varchar(255)  default ''                null comment '请求地址',
    user_ip        varchar(50)                             null comment '用户 IP',
    user_agent     varchar(200)                            null comment '浏览器 UA',
    creator        varchar(64)   default ''                null comment '创建者',
    create_time    datetime      default CURRENT_TIMESTAMP not null comment '创建时间',
    updater        varchar(64)   default ''                null comment '更新者',
    update_time    datetime      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted        bit           default b'0'              not null comment '是否删除',
    tenant_id      bigint        default 0                 not null comment '租户编号'
)
    comment '操作日志记录 V2 版本' collate = utf8mb4_unicode_ci
                                   row_format = DYNAMIC;

create table system_post
(
    id          bigint auto_increment comment '岗位ID'
        primary key,
    code        varchar(64)                           not null comment '岗位编码',
    name        varchar(50)                           not null comment '岗位名称',
    sort        int                                   not null comment '显示顺序',
    status      tinyint                               not null comment '状态（0正常 1停用）',
    remark      varchar(500)                          null comment '备注',
    creator     varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit         default b'0'              not null comment '是否删除',
    tenant_id   bigint      default 0                 not null comment '租户编号'
)
    comment '岗位信息表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table system_role
(
    id                  bigint auto_increment comment '角色ID'
        primary key,
    name                varchar(30)                            not null comment '角色名称',
    code                varchar(100)                           not null comment '角色权限字符串',
    sort                int                                    not null comment '显示顺序',
    data_scope          tinyint      default 1                 not null comment '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
    data_scope_dept_ids varchar(500) default ''                not null comment '数据范围(指定部门数组)',
    status              tinyint                                not null comment '角色状态（0正常 1停用）',
    type                tinyint                                not null comment '角色类型',
    remark              varchar(500)                           null comment '备注',
    creator             varchar(64)  default ''                null comment '创建者',
    create_time         datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater             varchar(64)  default ''                null comment '更新者',
    update_time         datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted             bit          default b'0'              not null comment '是否删除',
    tenant_id           bigint       default 0                 not null comment '租户编号'
)
    comment '角色信息表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table system_role_menu
(
    id          bigint auto_increment comment '自增编号'
        primary key,
    role_id     bigint                                not null comment '角色ID',
    menu_id     bigint                                not null comment '菜单ID',
    creator     varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit         default b'0'              not null comment '是否删除',
    tenant_id   bigint      default 0                 not null comment '租户编号'
)
    comment '角色和菜单关联表' collate = utf8mb4_unicode_ci
                               row_format = DYNAMIC;

create table system_sms_channel
(
    id           bigint auto_increment comment '编号'
        primary key,
    signature    varchar(12)                           not null comment '短信签名',
    code         varchar(63)                           not null comment '渠道编码',
    status       tinyint                               not null comment '开启状态',
    remark       varchar(255)                          null comment '备注',
    api_key      varchar(128)                          not null comment '短信 API 的账号',
    api_secret   varchar(128)                          null comment '短信 API 的秘钥',
    callback_url varchar(255)                          null comment '短信发送回调 URL',
    creator      varchar(64) default ''                null comment '创建者',
    create_time  datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater      varchar(64) default ''                null comment '更新者',
    update_time  datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted      bit         default b'0'              not null comment '是否删除'
)
    comment '短信渠道' collate = utf8mb4_unicode_ci
                       row_format = DYNAMIC;

create table system_sms_code
(
    id          bigint auto_increment comment '编号'
        primary key,
    mobile      varchar(11)                           not null comment '手机号',
    code        varchar(6)                            not null comment '验证码',
    create_ip   varchar(15)                           not null comment '创建 IP',
    scene       tinyint                               not null comment '发送场景',
    today_index tinyint                               not null comment '今日发送的第几条',
    used        tinyint                               not null comment '是否使用',
    used_time   datetime                              null comment '使用时间',
    used_ip     varchar(255)                          null comment '使用 IP',
    creator     varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit         default b'0'              not null comment '是否删除',
    tenant_id   bigint      default 0                 not null comment '租户编号'
)
    comment '手机验证码' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create index idx_mobile
    on system_sms_code (mobile)
    comment '手机号';

create table system_sms_log
(
    id               bigint auto_increment comment '编号'
        primary key,
    channel_id       bigint                                not null comment '短信渠道编号',
    channel_code     varchar(63)                           not null comment '短信渠道编码',
    template_id      bigint                                not null comment '模板编号',
    template_code    varchar(63)                           not null comment '模板编码',
    template_type    tinyint                               not null comment '短信类型',
    template_content varchar(255)                          not null comment '短信内容',
    template_params  varchar(255)                          not null comment '短信参数',
    api_template_id  varchar(63)                           not null comment '短信 API 的模板编号',
    mobile           varchar(11)                           not null comment '手机号',
    user_id          bigint                                null comment '用户编号',
    user_type        tinyint                               null comment '用户类型',
    send_status      tinyint     default 0                 not null comment '发送状态',
    send_time        datetime                              null comment '发送时间',
    api_send_code    varchar(63)                           null comment '短信 API 发送结果的编码',
    api_send_msg     varchar(255)                          null comment '短信 API 发送失败的提示',
    api_request_id   varchar(255)                          null comment '短信 API 发送返回的唯一请求 ID',
    api_serial_no    varchar(255)                          null comment '短信 API 发送返回的序号',
    receive_status   tinyint     default 0                 not null comment '接收状态',
    receive_time     datetime                              null comment '接收时间',
    api_receive_code varchar(63)                           null comment 'API 接收结果的编码',
    api_receive_msg  varchar(255)                          null comment 'API 接收结果的说明',
    creator          varchar(64) default ''                null comment '创建者',
    create_time      datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater          varchar(64) default ''                null comment '更新者',
    update_time      datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted          bit         default b'0'              not null comment '是否删除'
)
    comment '短信日志' collate = utf8mb4_unicode_ci
                       row_format = DYNAMIC;

create table system_sms_template
(
    id              bigint auto_increment comment '编号'
        primary key,
    type            tinyint                               not null comment '模板类型',
    status          tinyint                               not null comment '开启状态',
    code            varchar(63)                           not null comment '模板编码',
    name            varchar(63)                           not null comment '模板名称',
    content         varchar(255)                          not null comment '模板内容',
    params          varchar(255)                          not null comment '参数数组',
    remark          varchar(255)                          null comment '备注',
    api_template_id varchar(63)                           not null comment '短信 API 的模板编号',
    channel_id      bigint                                not null comment '短信渠道编号',
    channel_code    varchar(63)                           not null comment '短信渠道编码',
    creator         varchar(64) default ''                null comment '创建者',
    create_time     datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater         varchar(64) default ''                null comment '更新者',
    update_time     datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         bit         default b'0'              not null comment '是否删除'
)
    comment '短信模板' collate = utf8mb4_unicode_ci
                       row_format = DYNAMIC;

create table system_social_client
(
    id            bigint auto_increment comment '编号'
        primary key,
    name          varchar(255)                          not null comment '应用名',
    social_type   tinyint                               not null comment '社交平台的类型',
    user_type     tinyint                               not null comment '用户类型',
    client_id     varchar(255)                          not null comment '客户端编号',
    client_secret varchar(255)                          not null comment '客户端密钥',
    agent_id      varchar(255)                          null comment '代理编号',
    status        tinyint                               not null comment '状态',
    creator       varchar(64) default ''                null comment '创建者',
    create_time   datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater       varchar(64) default ''                null comment '更新者',
    update_time   datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted       bit         default b'0'              not null comment '是否删除',
    tenant_id     bigint      default 0                 not null comment '租户编号'
)
    comment '社交客户端表' collate = utf8mb4_unicode_ci
                           row_format = DYNAMIC;

create table system_social_user
(
    id             bigint unsigned auto_increment comment '主键(自增策略)'
        primary key,
    type           tinyint                               not null comment '社交平台的类型',
    openid         varchar(32)                           not null comment '社交 openid',
    token          varchar(256)                          null comment '社交 token',
    raw_token_info varchar(1024)                         not null comment '原始 Token 数据，一般是 JSON 格式',
    nickname       varchar(32)                           not null comment '用户昵称',
    avatar         varchar(255)                          null comment '用户头像',
    raw_user_info  varchar(1024)                         not null comment '原始用户数据，一般是 JSON 格式',
    code           varchar(256)                          not null comment '最后一次的认证 code',
    state          varchar(256)                          null comment '最后一次的认证 state',
    creator        varchar(64) default ''                null comment '创建者',
    create_time    datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater        varchar(64) default ''                null comment '更新者',
    update_time    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted        bit         default b'0'              not null comment '是否删除',
    tenant_id      bigint      default 0                 not null comment '租户编号'
)
    comment '社交用户表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table system_social_user_bind
(
    id             bigint unsigned auto_increment comment '主键(自增策略)'
        primary key,
    user_id        bigint                                not null comment '用户编号',
    user_type      tinyint                               not null comment '用户类型',
    social_type    tinyint                               not null comment '社交平台的类型',
    social_user_id bigint                                not null comment '社交用户的编号',
    creator        varchar(64) default ''                null comment '创建者',
    create_time    datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater        varchar(64) default ''                null comment '更新者',
    update_time    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted        bit         default b'0'              not null comment '是否删除',
    tenant_id      bigint      default 0                 not null comment '租户编号'
)
    comment '社交绑定表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table system_tenant
(
    id              bigint auto_increment comment '租户编号'
        primary key,
    name            varchar(30)                            not null comment '租户名',
    contact_user_id bigint                                 null comment '联系人的用户编号',
    contact_name    varchar(30)                            not null comment '联系人',
    contact_mobile  varchar(500)                           null comment '联系手机',
    status          tinyint      default 0                 not null comment '租户状态（0正常 1停用）',
    website         varchar(256) default ''                null comment '绑定域名',
    package_id      bigint                                 not null comment '租户套餐编号',
    expire_time     datetime                               not null comment '过期时间',
    account_count   int                                    not null comment '账号数量',
    creator         varchar(64)  default ''                not null comment '创建者',
    create_time     datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater         varchar(64)  default ''                null comment '更新者',
    update_time     datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         bit          default b'0'              not null comment '是否删除'
)
    comment '租户表' collate = utf8mb4_unicode_ci
                     row_format = DYNAMIC;

create table system_tenant_package
(
    id          bigint auto_increment comment '套餐编号'
        primary key,
    name        varchar(30)                            not null comment '套餐名',
    status      tinyint      default 0                 not null comment '租户状态（0正常 1停用）',
    remark      varchar(256) default ''                null comment '备注',
    menu_ids    varchar(4096)                          not null comment '关联的菜单编号',
    creator     varchar(64)  default ''                not null comment '创建者',
    create_time datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64)  default ''                null comment '更新者',
    update_time datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit          default b'0'              not null comment '是否删除'
)
    comment '租户套餐表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table system_user_post
(
    id          bigint auto_increment comment 'id'
        primary key,
    user_id     bigint      default 0                 not null comment '用户ID',
    post_id     bigint      default 0                 not null comment '岗位ID',
    creator     varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit         default b'0'              not null comment '是否删除',
    tenant_id   bigint      default 0                 not null comment '租户编号'
)
    comment '用户岗位表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;

create table system_user_role
(
    id          bigint auto_increment comment '自增编号'
        primary key,
    user_id     bigint                                not null comment '用户ID',
    role_id     bigint                                not null comment '角色ID',
    creator     varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    updater     varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit         default b'0'              null comment '是否删除',
    tenant_id   bigint      default 0                 not null comment '租户编号'
)
    comment '用户和角色关联表' collate = utf8mb4_unicode_ci
                               row_format = DYNAMIC;

create table system_users
(
    id          bigint auto_increment comment '用户ID'
        primary key,
    username    varchar(30)                            not null comment '用户账号',
    password    varchar(100) default ''                not null comment '密码',
    nickname    varchar(30)                            not null comment '用户昵称',
    remark      varchar(500)                           null comment '备注',
    dept_id     bigint                                 null comment '部门ID',
    post_ids    varchar(255)                           null comment '岗位编号数组',
    email       varchar(50)  default ''                null comment '用户邮箱',
    mobile      varchar(11)  default ''                null comment '手机号码',
    sex         tinyint      default 0                 null comment '用户性别',
    avatar      varchar(512) default ''                null comment '头像地址',
    status      tinyint      default 0                 not null comment '帐号状态（0正常 1停用）',
    login_ip    varchar(50)  default ''                null comment '最后登录IP',
    login_date  datetime                               null comment '最后登录时间',
    creator     varchar(64)  default ''                null comment '创建者',
    create_time datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updater     varchar(64)  default ''                null comment '更新者',
    update_time datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     bit          default b'0'              not null comment '是否删除',
    tenant_id   bigint       default 0                 not null comment '租户编号'
)
    comment '用户信息表' collate = utf8mb4_unicode_ci
                         row_format = DYNAMIC;


