/*
 * Copyright 2017 Colin Fleming
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

package cursive

import org.assertj.core.api.KotlinAssertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test

class IncrementalCompilationTest : IntegrationTestBase() {
  val coreNsSourceFile = testProjectDir.resolve("src/main/clojure/basic_project/core.clj")

  @Test
  fun incrementalCompileTaskUpToDateWhenNoChangesNoAot() {
    // when
    val firstRunResult = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(firstRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    println(firstRunResult.output)
    assertSourceFileCopiedToOutputDir(coreNsSourceFile)

    // when
    val secondRunResult = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(secondRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    println(secondRunResult.output)
    assertSourceFileCopiedToOutputDir(coreNsSourceFile)
  }

  @Test
  fun incrementalCompileTaskUpToDateWhenNoChangesWithAot() {
    // given
    projectGradleFile().appendText("compileClojure.aotCompile = true")

    // when
    val firstRunResult = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(firstRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    println(firstRunResult.output)
    assertSourceFileCompiledToOutputDir(coreNsSourceFile)

    // when
    val secondRunResult = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(secondRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    println(secondRunResult.output)
    assertSourceFileCompiledToOutputDir(coreNsSourceFile)
  }

  @Test
  fun incrementalCompileTaskExecutedWhenSourceFileChangedNoAot() {
    // given
    val utilsNsSourceFile = testProjectDir.resolve("src/main/clojure/basic_project/utils.clj")

    // when
    val firstRunResult = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(firstRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    println(firstRunResult.output)
    assertSourceFileCopiedToOutputDir(coreNsSourceFile)
    assertThat(destinationFileForSourceFile(utilsNsSourceFile).exists()).isFalse()

    coreNsSourceFile.delete()
    utilsNsSourceFile.writeText("""(ns basic-project.utils) (defn ping [] "pong")""")

    // when
    val secondRunResult = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(secondRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    println(secondRunResult.output)
    assertThat(destinationFileForSourceFile(coreNsSourceFile).exists()).isFalse()
    assertSourceFileCopiedToOutputDir(utilsNsSourceFile)
  }

  @Test
  fun incrementalCompileTaskExecutedWhenNewCljFileAddedWithAot() {
    // given
    projectGradleFile().appendText("compileClojure.aotCompile = true")
    val utilsNsSourceFile = testProjectDir.resolve("src/main/clojure/basic_project/utils.clj")

    // when
    val firstRunResult = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(firstRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    println(firstRunResult.output)
    assertSourceFileCompiledToOutputDir(coreNsSourceFile)
    assertThat(compiledClassFilesForSourceFile(utilsNsSourceFile)).isEmpty()

    coreNsSourceFile.delete()
    utilsNsSourceFile.writeText("""(ns basic-project.utils) (defn ping [] "pong")""")

    // when
    val secondRunResult = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(secondRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    println(secondRunResult.output)
    assertThat(compiledClassFilesForSourceFile(coreNsSourceFile)).isEmpty()
    assertSourceFileCompiledToOutputDir(utilsNsSourceFile)
  }
}