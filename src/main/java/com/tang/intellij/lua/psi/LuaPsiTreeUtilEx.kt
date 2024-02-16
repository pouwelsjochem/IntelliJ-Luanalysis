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

package com.tang.intellij.lua.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.Processor
import com.tang.intellij.lua.stubs.LuaFileStub

typealias STUB_ELE = StubElement<*>
typealias STUB_PSI = StubBasedPsiElement<*>

object LuaPsiTreeUtilEx {

    private fun STUB_ELE.walkUp(curChild: STUB_ELE, processor: Processor<STUB_ELE>) {
        val list = this.childrenStubs
        val index = list.indexOf(curChild)
        if (index > 0) {
            for (i in 0 until index) {
                val element = list[index - i - 1]
                if (!processor.process(element)) break
            }
        }
    }

    private fun <T> findStubOfType(stub: STUB_ELE, clazz: Class<T>, collector: (t:T) -> Boolean) {
        val list = stub.childrenStubs
        for (i in 0 until list.size) {
            val stubElement = list[i]
            if (clazz.isInstance(stubElement.psi)) {
                val t = clazz.cast(stubElement.psi)
                collector(t)
            } else findStubOfType(stubElement, clazz, collector)
        }
    }

    fun walkUpNameDef(psi: PsiElement?, processor: Processor<PsiNamedElement>, nameExprProcessor: Processor<LuaAssignStat>? = null) {
        if (psi == null) return

        var continueSearch = true
        if (psi is STUB_PSI) {
            val stub = psi.stub
            if (stub != null) {
                var cur: STUB_ELE = stub
                do {
                    val scope = cur.parentStub
                    scope.walkUp(cur) { next ->
                        val element = next.psi
                        when (element) {
                            is LuaLocalDefStat -> {
                                findStubOfType(next, LuaLocalDef::class.java) {
                                    continueSearch = processor.process(it)
                                    continueSearch
                                }
                            }
                            is LuaParamDef -> continueSearch = processor.process(element)
                            is LuaLocalFuncDefStat -> continueSearch = processor.process(element)
                            else -> { }
                        }
                        continueSearch
                    }

                    if (scope is LuaFileStub)
                        break
                    cur = scope
                } while (continueSearch)

                continueSearch = false
            }
        }

        if (continueSearch)
            walkUpPsiLocalName(psi, processor, nameExprProcessor)
    }

    /**
     * 向上寻找 local 定义
     * @param element 当前搜索起点
     * @param processor 处理器
     */
    private fun walkUpPsiLocalName(element: PsiElement, processor: Processor<PsiNamedElement>, nameExprProcessor: Processor<LuaAssignStat>?) {
        var curr: PsiElement = element.realContext
        do {
            var continueSearch = true
            val prev = curr.prevSibling
            if (prev == null) {
                curr = curr.parent
                if (curr is LuaLocalDefStat) {
                    continue
                }
            } else curr = prev

            continueSearch = when (curr) {
                is LuaLocalDefStat -> {
                    curr.localDefList.none {
                        processor.process(it)
                    }
                }
                is LuaParamDef -> processor.process(curr)
                is LuaLocalFuncDefStat -> processor.process(curr)
                is LuaAssignStat -> nameExprProcessor?.process(curr) ?: true
                else -> true
            }
        } while (continueSearch && curr !is PsiFile)
    }

    fun walkUpLocalFuncDefStat(psi: PsiElement, processor: Processor<LuaLocalFuncDefStat>) {
        var continueSearch = true
        if (psi is STUB_PSI) {
            val stub = psi.stub
            if (stub != null) {
                var cur: STUB_ELE = stub
                do {
                    val scope = cur.parentStub
                    scope.walkUp(cur) { next ->
                        val psiElement = next.psi
                        if (psiElement is LuaLocalFuncDefStat) {
                            continueSearch = processor.process(psiElement)
                        }
                        continueSearch
                    }
                    if (scope is LuaFileStub)
                        break
                    cur = scope
                } while (continueSearch)
                continueSearch = false
            }
        }
        if (continueSearch)
            walkUpPsiLocalFunc(psi, processor)
    }

    /**
     * 向上寻找 local function 定义
     * @param current 当前搜导起点
     * @param processor 处理器
     */
    private fun walkUpPsiLocalFunc(current: PsiElement, processor: Processor<LuaLocalFuncDefStat>) {
        var continueSearch = true
        var curr = current
        do {
            if (curr is LuaLocalFuncDefStat)
                continueSearch = processor.process(curr)

            curr = curr.prevSibling ?: curr.parent
        } while (continueSearch && curr !is PsiFile)
    }

    fun <T : PsiElement> processChildren(element: PsiElement?, childClass: Class<T>, processor: Processor<T>): Boolean {
        if (element is STUB_PSI) {
            val stub = element.stub
            if (stub != null) {
                for (child in stub.childrenStubs) {
                    if (childClass.isInstance(child)) {
                        @Suppress("UNCHECKED_CAST")
                        if (!processor.process(child as T)) {
                            return false
                        }
                    }
                }
                return true
            }
        }

        var child = element?.firstChild
        while (child != null) {
            if (childClass.isInstance(child)) {
                @Suppress("UNCHECKED_CAST")
                if (!processor.process(child as T)) {
                    return false
                }
            }
            child = child.nextSibling
        }
        return true
    }

    fun processChildren(element: PsiElement?, processor: Processor<PsiElement>): Boolean {
        if (element is STUB_PSI) {
            val stub = element.stub

            if (stub != null) {
                for (child in stub.childrenStubs) {
                    if (!processor.process(child.psi))
                        return false
                }
                return true
            }
        }

        var child = element?.firstChild
        while (child != null) {
            if (!processor.process(child)) {
                return false
            }
            child = child.nextSibling
        }
        return true
    }
}
