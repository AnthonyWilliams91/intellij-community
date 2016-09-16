/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.junit5;

import com.intellij.codeInsight.TestFrameworks;
import com.intellij.execution.junit.JUnit5Framework;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.TestRunnerUtil;
import com.intellij.testFramework.fixtures.*;
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl;
import com.intellij.testIntegration.TestFramework;
import com.intellij.util.ThrowableRunnable;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class JUnit5AcceptanceTest {

  private JavaCodeInsightTestFixture myFixture;

  @BeforeEach
  void setUp() throws Exception {
    IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
    TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = factory.createLightFixtureBuilder(new DefaultLightProjectDescriptor());
    final IdeaProjectTestFixture fixture = fixtureBuilder.getFixture();
    myFixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture, new LightTempDirTestFixtureImpl(true));
    myFixture.setUp();
  }

  @AfterEach
  void tearDown() throws Exception {
    myFixture.tearDown();
  }

  @Test
  void testFactoryMethods() {
    doTest(() -> {
      PsiClass aClass = myFixture.addClass("class MyTest {@org.junit.jupiter.api.TestFactory java.util.List<org.junit.jupiter.api.DynamicTest> tests() {return null;}}");
      PsiMethod factoryMethod = aClass.getMethods()[0];
      assertNotNull(factoryMethod);
      assertTrue(JUnitUtil.isTestAnnotated(factoryMethod));
    });
  }

  @Test
  void testDefaultMethodInInterface() {
    doTest(() -> {
      PsiClass aClass = myFixture.addClass("interface MyTest {@org.junit.jupiter.api.Test default void method() {}}");
      assertTrue(JUnitUtil.isTestClass(aClass, false, false));
    });
  }

  @Test
  void testFrameworkDetection() {
    doTest(() -> {
      PsiClass aClass = myFixture.addClass("class MyTest {@org.junit.jupiter.api.Test void method() {}}");
      assertNotNull(aClass);
      TestFramework framework = TestFrameworks.detectFramework(aClass);
      assertTrue(framework instanceof JUnit5Framework, framework.getName());
    });
  }

  @Test
  void methodPresentations() {
    doTest(() -> {
      PsiClass aClass = myFixture.addClass("class MyTest {" +
                                           "  @org.junit.jupiter.api.Test void method() {}" +
                                           "  @org.junit.jupiter.api.Test void method(a.TestInfo info) {}" +
                                           "  @org.junit.Test void method1() {}" +
                                           "  @org.junit.Test void method1(a.TestInfo info) {}" +
                                           "}");
      assertNotNull(aClass);

      Stream<String> expectedData = Arrays.stream(new String[]{"method", "method(a.TestInfo)", "method1", "method1"});
      StreamEx.of(aClass.getMethods())
        .zipWith(expectedData)
        .forEach(e -> assertEquals(e.getValue(), JUnitConfiguration.Data.getMethodPresentation(e.getKey())));
    });
  }

  @Test
  void junit5LibraryAdjustments() {
    doTest(() -> {
      myFixture.configureByText("MyTest.java", "class MyTest {@org.<error descr=\"Cannot resolve symbol 'junit'\">junit</error>.jupiter.api.Te<caret>st void method() {}}");
      myFixture.testHighlighting(false, false, false);
      final Set<String> frameworks = myFixture.getAllQuickFixes().stream()
        .map(action -> action.getText())
        .filter(name -> name.startsWith("Add")).collect(Collectors.toSet());
      assertAll("Detected frameworks: " + frameworks.toString(),
                () -> assertTrue(frameworks.contains("Add 'JUnit5' to classpath")),
                () -> assertTrue(frameworks.contains("Add 'JUnit4' to classpath")));

      myFixture.configureByText("MyTest.java", "class MyTest {@<error descr=\"Cannot resolve symbol 'DisplayName'\">DisplayName</error> void method() {}}");
      myFixture.testHighlighting(false, false, false);

      Set<String> displayNameFrameworks = myFixture.getAllQuickFixes().stream()
        .map(action -> action.getText())
        .filter(name -> name.startsWith("Add")).collect(Collectors.toSet());
      assertAll("Detected frameworks: " + displayNameFrameworks.toString(),
                () -> assertTrue (displayNameFrameworks.contains("Add 'JUnit5' to classpath")),
                () -> assertFalse(displayNameFrameworks.contains("Add 'JUnit4' to classpath")));

    });
  }

  private static void doTest(ThrowableRunnable<Throwable> run) {
    TestRunnerUtil.replaceIdeEventQueueSafely();
    EdtTestUtil.runInEdtAndWait(run);
  }
}
