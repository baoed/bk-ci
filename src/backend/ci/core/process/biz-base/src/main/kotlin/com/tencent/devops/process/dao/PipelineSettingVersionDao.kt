/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.dao

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.model.process.tables.TPipelineSettingVersion
import com.tencent.devops.model.process.tables.records.TPipelineSettingVersionRecord
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.util.NotifyTemplateUtils
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_DEFAULT
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class PipelineSettingVersionDao {

    // 新流水线创建的时候，设置默认的通知配置。
    fun insertNewSetting(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        version: Int,
        isTemplate: Boolean = false
    ): Int {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                NAME,
                RUN_LOCK_TYPE,
                DESC,
                SUCCESS_RECEIVER,
                FAIL_RECEIVER,
                SUCCESS_GROUP,
                FAIL_GROUP,
                SUCCESS_TYPE,
                FAIL_TYPE,
                SUCCESS_CONTENT,
                FAIL_CONTENT,
                WAIT_QUEUE_TIME_SECOND,
                MAX_QUEUE_SIZE,
                IS_TEMPLATE,
                VERSION
            )
                .values(
                    projectId,
                    pipelineId,
                    pipelineName,
                    PipelineRunLockType.toValue(PipelineRunLockType.MULTIPLE),
                    "",
                    "\${$PIPELINE_START_USER_NAME}",
                    "\${$PIPELINE_START_USER_NAME}",
                    "",
                    "",
                    "EMAIL,RTX",
                    "EMAIL,RTX",
                    NotifyTemplateUtils.COMMON_SHUTDOWN_SUCCESS_CONTENT,
                    NotifyTemplateUtils.COMMON_SHUTDOWN_FAILURE_CONTENT,
                    DateTimeUtil.minuteToSecond(PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_DEFAULT),
                    PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT,
                    isTemplate,
                    version
                )
                .execute()
        }
    }

    fun saveSetting(dslContext: DSLContext, setting: PipelineSetting, version: Int, isTemplate: Boolean = false): Int {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                NAME,
                DESC,
                RUN_LOCK_TYPE,
                PIPELINE_ID,
                SUCCESS_RECEIVER,
                FAIL_RECEIVER,
                SUCCESS_GROUP,
                FAIL_GROUP,
                SUCCESS_TYPE,
                FAIL_TYPE,
                FAIL_WECHAT_GROUP_FLAG,
                FAIL_WECHAT_GROUP,
                SUCCESS_WECHAT_GROUP_FLAG,
                SUCCESS_WECHAT_GROUP,
                SUCCESS_DETAIL_FLAG,
                FAIL_DETAIL_FLAG,
                SUCCESS_CONTENT,
                FAIL_CONTENT,
                WAIT_QUEUE_TIME_SECOND,
                MAX_QUEUE_SIZE,
                IS_TEMPLATE,
                VERSION
            )
                .values(
                    setting.projectId,
                    setting.pipelineName,
                    setting.desc,
                    PipelineRunLockType.toValue(setting.runLockType),
                    setting.pipelineId,
                    setting.successSubscription.users,
                    setting.failSubscription.users,
                    setting.successSubscription.groups.joinToString(","),
                    setting.failSubscription.groups.joinToString(","),
                    setting.successSubscription.types.joinToString(",") { it.name },
                    setting.failSubscription.types.joinToString(",") { it.name },
                    setting.failSubscription.wechatGroupFlag,
                    setting.failSubscription.wechatGroup,
                    setting.successSubscription.wechatGroupFlag,
                    setting.successSubscription.wechatGroup,
                    setting.successSubscription.detailFlag,
                    setting.failSubscription.detailFlag,
                    setting.successSubscription.content,
                    setting.failSubscription.content,
                    DateTimeUtil.minuteToSecond(setting.waitQueueTimeMinute),
                    setting.maxQueueSize,
                    isTemplate,
                    version
                )
                .execute()
        }
    }

    fun getSetting(dslContext: DSLContext, pipelineId: String, version: Int): TPipelineSettingVersionRecord? {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.eq(version))
                .fetchOne()
        }
    }

    fun getSettings(dslContext: DSLContext, pipelineIds: Set<String>): Result<TPipelineSettingVersionRecord> {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.`in`(pipelineIds))
                .fetch()
        }
    }

    fun getSetting(dslContext: DSLContext, pipelineIds: Collection<String>): Result<TPipelineSettingVersionRecord> {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.`in`(pipelineIds))
                .fetch()
        }
    }

    fun getSetting(
        dslContext: DSLContext,
        projectId: String,
        name: String,
        pipelineId: String?,
        isTemplate: Boolean = false
    ): Result<TPipelineSettingVersionRecord> {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            val conditions =
                mutableListOf<Condition>(
                    PROJECT_ID.eq(projectId),
                    NAME.eq(name),
                    IS_TEMPLATE.eq(isTemplate)
                ) // 只比较非模板的设置
            if (!pipelineId.isNullOrBlank()) conditions.add(PIPELINE_ID.ne(pipelineId))
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
        }
    }

    /**
     * 更新模版引用的设置
     */
    fun updateSettingName(dslContext: DSLContext, pipelineIdList: List<String>, name: String) {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            dslContext.update(this)
                .set(NAME, name)
                .where(PIPELINE_ID.`in`(pipelineIdList))
                .execute()
        }
    }

    fun updateSetting(dslContext: DSLContext, pipelineId: String, name: String, desc: String) {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            dslContext.update(this)
                .set(NAME, name)
                .set(DESC, desc)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun updateSetting(dslContext: DSLContext, pipelineId: String, version: Int, name: String, desc: String) {
            with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
                dslContext.update(this)
                    .set(NAME, name)
                    .set(DESC, desc)
                    .where(PIPELINE_ID.eq(pipelineId))
                    .and(VERSION.eq(version))
                    .execute()
            }
        }

    fun getSettingByName(
        dslContext: DSLContext,
        name: String,
        projectId: String,
        pipelineId: String,
        isTemplate: Boolean = false
    ): Record1<Int>? {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.selectCount()
                .from(this)
                .where(
                    PROJECT_ID.eq(projectId).and(NAME.eq(name)).and(PIPELINE_ID.ne(pipelineId)).and(
                        IS_TEMPLATE.eq(
                            isTemplate
                        )
                    )
                )
                .fetchOne()
        }
    }

    fun delete(dslContext: DSLContext, pipelineId: String): Int {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }
}