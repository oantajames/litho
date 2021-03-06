/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class LifecycleMethodsTest {

  private enum LifecycleStep {
    ON_CREATE_LAYOUT,
    ON_PREPARE,
    ON_MEASURE,
    ON_BOUNDS_DEFINED,
    ON_CREATE_MOUNT_CONTENT,
    ON_MOUNT,
    ON_BIND,
    ON_UNBIND,
    ON_UNMOUNT
  }

  private LithoView mLithoView;
  private ComponentTree mComponentTree;
  private LifecycleMethodsComponent mLifecycle;
  private LifecycleMethodsInstance mComponent;

  @Before
  public void setup() throws Exception {
    mLithoView = new LithoView(RuntimeEnvironment.application);
    mLifecycle = new LifecycleMethodsComponent();
    mComponent = mLifecycle.create(10);

    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);
    mComponentTree = ComponentTree.create(c, mComponent)
        .incrementalMount(false)
        .layoutDiffing(false)
        .build();
    mLithoView.setComponentTree(mComponentTree);
  }

  @Test
  public void testLifecycle() {
    mLithoView.onAttachedToWindow();
    ComponentTestHelper.measureAndLayout(mLithoView);

    assertEquals(LifecycleStep.ON_BIND, mComponent.getCurrentStep());

    mLithoView.onDetachedFromWindow();
    assertEquals(LifecycleStep.ON_UNBIND, mComponent.getCurrentStep());

    mLithoView.onAttachedToWindow();
    assertEquals(LifecycleStep.ON_BIND, mComponent.getCurrentStep());

    mComponentTree.setRoot(mLifecycle.create(20));
    ComponentTestHelper.measureAndLayout(mLithoView);
    assertEquals(LifecycleStep.ON_UNMOUNT, mComponent.getCurrentStep());

    mComponentTree.setRoot(mComponent);
    ComponentTestHelper.measureAndLayout(mLithoView);
    assertEquals(LifecycleStep.ON_BIND, mComponent.getCurrentStep());

    mLithoView.onDetachedFromWindow();
    mComponentTree.setRoot(mComponent);
    ComponentTestHelper.measureAndLayout(mLithoView);
    assertEquals(LifecycleStep.ON_UNBIND, mComponent.getCurrentStep());
  }

  private class LifecycleMethodsComponent extends ComponentLifecycle {

    @Override
    protected ComponentLayout onCreateLayout(ComponentContext c, Component<?> component) {
      LifecycleMethodsInstance instance = (LifecycleMethodsInstance) component;
      instance.setCurrentStep(LifecycleStep.ON_CREATE_LAYOUT);

      return super.onCreateLayout(c, component);
    }

    @Override
    protected void onPrepare(ComponentContext c, Component<?> component) {
      LifecycleMethodsInstance instance = (LifecycleMethodsInstance) component;
      instance.setCurrentStep(LifecycleStep.ON_PREPARE);
    }

    @Override
    protected boolean canMeasure() {
      return true;
    }

    @Override
    protected void onMeasure(
        ComponentContext c,
        ComponentLayout layout,
        int widthSpec,
        int heightSpec,
        Size size,
        Component<?> component) {
      LifecycleMethodsInstance instance = (LifecycleMethodsInstance) component;
      instance.setCurrentStep(LifecycleStep.ON_MEASURE);

      size.width = instance.getSize();
      size.height = instance.getSize();
    }

    @Override
    protected void onBoundsDefined(
        ComponentContext c,
        ComponentLayout layout,
        Component<?> component) {
      LifecycleMethodsInstance instance = (LifecycleMethodsInstance) component;
      instance.setCurrentStep(LifecycleStep.ON_BOUNDS_DEFINED);
    }

    @Override
    protected Object onCreateMountContent(ComponentContext context) {
      mComponent.setCurrentStep(LifecycleStep.ON_CREATE_MOUNT_CONTENT);

      return new LifecycleMethodsDrawable(mComponent);
    }

    @Override
    protected void onMount(
        ComponentContext c,
        Object convertContent,
        Component<?> component) {
      LifecycleMethodsInstance instance = (LifecycleMethodsInstance) component;
      instance.setCurrentStep(LifecycleStep.ON_MOUNT);
      final LifecycleMethodsDrawable d = (LifecycleMethodsDrawable) convertContent;

      d.setComponent(instance);
    }

    @Override
    protected void onUnmount(
        ComponentContext c,
        Object mountedContent,
        Component<?> component) {
      LifecycleMethodsInstance instance = (LifecycleMethodsInstance) component;
      instance.setCurrentStep(LifecycleStep.ON_UNMOUNT);
    }

    @Override
    protected void onBind(
        ComponentContext c,
        Object mountedContent,
        Component<?> component) {
      LifecycleMethodsInstance instance = (LifecycleMethodsInstance) component;
      instance.setCurrentStep(LifecycleStep.ON_BIND);
    }

    @Override
    protected void onUnbind(
        ComponentContext c,
        Object mountedContent,
        Component<?> component) {
      LifecycleMethodsInstance instance = (LifecycleMethodsInstance) component;
      instance.setCurrentStep(LifecycleStep.ON_UNBIND);
    }

    @Override
    protected boolean shouldUpdate(Component previous, Component next) {
      return true;
    }

    @Override
    public MountType getMountType() {
      return MountType.DRAWABLE;
    }

    public LifecycleMethodsInstance create(int size) {
      return new LifecycleMethodsInstance(this, size);
    }
  }

  private static class LifecycleMethodsInstance
      extends Component<LifecycleMethodsComponent> implements Cloneable {

    private final int mSize;
    LifecycleStep mCurrentStep = LifecycleStep.ON_UNMOUNT;

    protected LifecycleMethodsInstance(LifecycleMethodsComponent l, int size) {
      super(l);
      mSize = size;
    }

    @Override
    public String getSimpleName() {
      return "LifecycleMethodsInstance";
    }

    LifecycleStep getCurrentStep() {
      return mCurrentStep;
    }

    void setCurrentStep(LifecycleStep currentStep) {
      switch (currentStep) {
        case ON_CREATE_LAYOUT:
          assertEquals(LifecycleStep.ON_UNMOUNT, mCurrentStep);
          break;

        case ON_PREPARE:
          assertEquals(LifecycleStep.ON_CREATE_LAYOUT, mCurrentStep);
          break;

        case ON_MEASURE:
          assertTrue(
              mCurrentStep == LifecycleStep.ON_PREPARE ||
              mCurrentStep == LifecycleStep.ON_MEASURE);
          break;

        case ON_BOUNDS_DEFINED:
          assertTrue(
              mCurrentStep == LifecycleStep.ON_PREPARE ||
              mCurrentStep == LifecycleStep.ON_MEASURE);
          break;

        case ON_CREATE_MOUNT_CONTENT:
          assertTrue(
              mCurrentStep == LifecycleStep.ON_BOUNDS_DEFINED);

        case ON_MOUNT:
          assertTrue(
              mCurrentStep == LifecycleStep.ON_BOUNDS_DEFINED ||
              mCurrentStep == LifecycleStep.ON_CREATE_MOUNT_CONTENT);
          break;

        case ON_BIND:
          assertTrue(
              mCurrentStep == LifecycleStep.ON_MOUNT ||
              mCurrentStep == LifecycleStep.ON_UNBIND);
          break;

        case ON_UNBIND:
          assertEquals(LifecycleStep.ON_BIND, mCurrentStep);
          break;

        case ON_UNMOUNT:
          assertEquals(LifecycleStep.ON_UNBIND, mCurrentStep);
          break;
      }

      mCurrentStep = currentStep;
    }

    int getSize() {
      return mSize;
    }

    @Override
    public Component<LifecycleMethodsComponent> makeShallowCopy() {
      return this;
    }
  }

  private static class LifecycleMethodsDrawable extends Drawable {

    private LifecycleMethodsInstance mComponent;

    private LifecycleMethodsDrawable(LifecycleMethodsInstance component) {
      assertEquals(LifecycleStep.ON_CREATE_MOUNT_CONTENT, component.getCurrentStep());
    }

    void setComponent(LifecycleMethodsInstance component) {
      mComponent = component;
      assertEquals(LifecycleStep.ON_MOUNT, mComponent.getCurrentStep());
    }

    @Override
    public void setBounds(int l, int t, int r, int b) {
      super.setBounds(l, t, r, b);

      assertTrue(
          mComponent.getCurrentStep() == LifecycleStep.ON_BIND ||
              mComponent.getCurrentStep() == LifecycleStep.ON_UNBIND);
    }

    @Override
    public void draw(Canvas canvas) {
      assertEquals(mComponent.getCurrentStep(), LifecycleStep.ON_BIND);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
      return 0;
    }
  }
}
