package com.tang.intellij.lua.editor

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import com.intellij.psi.stubs.StubIndex
import com.tang.intellij.lua.comment.psi.LuaDocTagAlias
import com.tang.intellij.lua.stubs.index.LuaAliasIndex

class LuaAliasNavigationContributor : ChooseByNameContributorEx {
    override fun processNames(processor: Processor<in String>,
                              scope: GlobalSearchScope,
                              filter: IdFilter?) {
        ContainerUtil.process(LuaAliasIndex.instance.getAllKeys(scope.project), processor)
    }

    override fun processElementsWithName(name: String,
                                         processor: Processor<in NavigationItem>,
                                         parameters: FindSymbolParameters) {
        val project = parameters.project
        val searchScope = parameters.searchScope
        val aliases = StubIndex.getElements(LuaAliasIndex.instance.key, name, project, searchScope, LuaDocTagAlias::class.java)
        ContainerUtil.process(aliases, processor)
    }
}
