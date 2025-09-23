/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.psi.search

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.application.ReadAction
import com.intellij.util.Processor
import com.intellij.util.concurrency.AppExecutorUtil
import com.tang.intellij.lua.ty.ITyClass
import com.tang.intellij.lua.ty.createSerializedClass

/**
 *
 * Created by tangzx on 2017/3/29.
 */
class LuaAllClassesSearchExecutor : QueryExecutorBase<ITyClass, LuaAllClassesSearch.SearchParameters>() {
    override fun processQuery(searchParameters: LuaAllClassesSearch.SearchParameters, processor: Processor<in ITyClass>) {
        val project = searchParameters.project

        ReadAction.nonBlocking<Unit> {
            LuaShortNamesManager.getInstance(project).processAllClasses(project) { typeName -> processor.process(createSerializedClass(typeName)) }
        }
            .inSmartMode(project)
            .submit(AppExecutorUtil.getAppExecutorService())
    }
}
