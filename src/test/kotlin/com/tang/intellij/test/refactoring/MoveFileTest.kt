/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.test.refactoring

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MoveFileTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/refactoring"

    fun testMoveFile() {
        // Load all files in the test folder so both A.lua and the 'to' directory exist
        myFixture.copyDirectoryToProject("", "") // copies entire refactoring/<testname> folder

        val fileToMove = "A.lua"
        val targetDirName = "to"

        val rootDir = myFixture.tempDirFixture.findOrCreateDir("")
        val child = rootDir.findFileByRelativePath(fileToMove)
        assertNotNull("File $fileToMove not found", child)

        val file = myFixture.psiManager.findFile(child!!)!!
        val child1 = rootDir.findChild(targetDirName)
        assertNotNull("Directory $targetDirName not found", child1)

        val targetDirectory = myFixture.psiManager.findDirectory(child1!!)
        assertNotNull("Failed to obtain directory reference to $targetDirName", targetDirectory)

        MoveFilesOrDirectoriesProcessor(
            project,
            arrayOf<PsiElement>(file),
            targetDirectory!!,
            false,
            false,
            null,
            null
        ).run()

        FileDocumentManager.getInstance().saveAllDocuments()
    }
}