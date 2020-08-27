# `APP`启动流程源码分析

[TOC]

## 一、`APP`应用本质揭秘

### 1.1 `Android`架构

从不同的角度可以把`Android`架构作如下划分：

![image](https://github.com/tianyalu/NeAppLauncherProcess/raw/master/show/android_structure.png)

### 1.2 `APP`之`Context`

`Android`中四大组件都拥有`Context`，`Context`非常重要，可以操作文件、数据库、`SharedPreferences`、`assets`等等。

![image](https://github.com/tianyalu/NeAppLauncherProcess/raw/master/show/app_context.png)

### 1.3 `Android`系统中的应用本质

在`Android`系统中，一个应用

> * 对应一个`process`进程；
> * 对应一个`package`；
> * 对应一个`dalvik VM`；
> * 对应一个`user`。

* `Android`系统最小的控制单元是进程`process`
* 应用/`CPU`最小的控制单元是线程

![image](https://github.com/tianyalu/NeAppLauncherProcess/raw/master/show/android_app_nature.png)

### 1.4 `JVM`、`DVM`和`ART`

`JVM`：即`Java Virtual Machine`,通过把平台无关的`.class`中的字节码翻译成平台无关的机器码来实现跨平台的`Java`虚拟机（基于`.class`）；

`DVM`：即`Dalvik Virtual Machine`, 使用`JIT(Just in time)`编译的`Android`虚拟机（基于`.dex`）；

`ART`：与`DVM`不同，它使用`AOT(Ahead of time)`编译的`Android`虚拟机（基于`.dex`）。

#### 1.4.1 `JVM`和`DVM`的区别

> 1. `JVM`公共解码`.class`文件来运行程序；`DVM`则是通过`.dex`文件。
> 2. `JVM`基于栈架构（指针引用）；`DVM`基于寄存器架构（句柄引用）。
> 3. `JVM`打包后，类单独加载，单独运行；`DVM`可执行文件体积更小，运行时共享相同的类，系统消耗小。

```bash
 JVM: .java -> javac -> .class -> jar -> .jar , 架构: 堆和栈的架构.
 DVM: .java -> javac -> .class -> dx.bat -> .dex , 架构: 寄存器(cpu上的一块高速缓存)。
```

#### 1.4.2 `DVM`和`ART`的区别

`DVM`使用`JIT`编译器；`ART`使用`AOT`编译器。

> `JIT(Just in time)`：每次应用在运行时，它实时地将一部分`Dalvik`字节码翻译成机器码；在程序的执行过程中，更多的代码被编译并缓存。由于`JIT`只翻译一部分代码，它消耗的内存更少，占用的物理存储空间也更少。
>
> `AOT(Ahead of time)`：在应用的安装期间，它就将`.dex`字节码翻译成机器码并存储在设备的存储器上。这个过程只在将应用安装到设备上时发生。由于不需要`JIT`编译，代码的执行速度要快得多。

#### 1.4.3 `ART`的优缺点

优点：

> 1. 系统性能显著提升，每次启动执行的时候，都可以直接运行，因此运行效率会更高；
> 2. 应用启动更快、运行更快、体验更流畅、触感反馈更及时；
> 3. 续航能力提升，因为应用程序每次运行时不用重复编译了，从而减少了`CPU`的使用频率，降低了能耗；
> 4. 支持更低的硬件。

缺点：

> 1. 更大的存储空间占用，可能增加10%~20%（字节码变为机器码之后，可能会增加10%~20%）；
> 2. 更长的应用安装时间，应用在第一次安装的时候，字节码就会预编译（`AOT`）成机器码，这样的话设备和应用的首次启动就会变慢。

## 二、`APP`启动流程

### 2.1 `Android`开机启动过程

从系统角度看，`Android`的启动程序可分为：

> 1. `bootloader`引导
>
> 2. 装载与启动`Linux`内核
>
> 3. 启动`Andorid`系统
>
>    3.1 启动`Init`进程
>
>    3.2 启动`Zygote`
>
>    3.3 启动`SystemService`
>
>    3.4 启动`Launcher`

![image](https://github.com/tianyalu/NeAppLauncherProcess/raw/master/show/android_start_process_structure.png)

### 2.1 系统启动`Launcher`的过程

`Linux kernel`启动以后会通过`init`进程来初始化`Android Runtime`的`Java`运行时环境，而`zygote`是`Android`的第一个进程，所有的`Andorid`应用以及大部分的系统服务都是通过这个进程`fork`出来的子进程（我现在看到的只有`native`的`ServiceManager`不是`zygote fork`出来的）。`zygote`进程`fork`出的系统进程其中之一是`SystemServer`进程，该进程启动的系统服务包含了`ActivityManagerService`进程，它主要管理`Activity`之间的跳转以及进程的生命周期。当`SystemServer`启动好所有的服务后，系统就进入`system ready`状态。当`ActivityManagerService`发现系统启动好后就会发出一个`Intent`:

```java
Intent getHomeIntent() {
  Intent intent = new Intent(mTopAction, mTopData != null ? Uri.parse(mTopData) : null);
  intent.setComponent(mTopComponent);
  intent.addFlags(Intent.FLAG_DEBUG_TRIAGED_MISSING);
  if (mFactoryTest != FactoryTest.FACTORY_TEST_LOW_LEVEL) {
    intent.addCategory(Intent.CATEGORY_HOME);
  }
  return intent;
}
```

通过这个`category`类型为`CATEGORY_HOME`的`intent`,`ActivityMagagerService`就会通过`startHomeActivityLocked(intent, aInfo, myReason)`方法启动`Home`进程了。而这个启动`Home`进程的过程实际上还是通过`zygote`来`fork`出的一个子进程。因此只要在 [清单文件](https://www.androidos.net.cn/android/8.0.0_r4/xref/packages/apps/Launcher3/AndroidManifest.xml) 中具备这样的`intent-filter`的都可以在开机的时候作为`Home`启动：

```xml
<activity
          android:name="com.android.launcher3.Launcher"
          android:launchMode="singleTask"
          android:clearTaskOnLaunch="true"
          android:stateNotNeeded="true"
          android:windowSoftInputMode="adjustPan|stateUnchanged"
          android:screenOrientation="nosensor"
          android:configChanges="keyboard|keyboardHidden|navigation"
          android:resizeableActivity="true"
          android:resumeWhilePausing="true"
          android:taskAffinity=""
          android:enabled="true">
  <intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.HOME" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.MONKEY"/>
  </intent-filter>
</activity>
```

总结`Launcher`启动流程可以用如下时序图概括：

![image](https://github.com/tianyalu/NeAppLauncherProcess/raw/master/show/launcher_start_sequence.png)

参考文献：
> * [Android Launcher 分析](https://blog.csdn.net/fengkehuan/article/details/6205980)
> * [Android源码解析 - Launcher启动流程](https://www.jianshu.com/p/b6fc483e6f71)
> * [Android启动流程——1序言、bootloader引导与Linux启动](https://www.jianshu.com/p/9f978d57c683)
### 2.1 `Launcher`启动应用的流程

[`Launcher`](https://www.androidos.net.cn/android/8.0.0_r4/xref/packages/apps/Launcher3)是`Android`中的一个系统应用，用来展现所有的应用程序。`Launcher`也是继承自`Activity`，其布局文件为`launcher.xml`，根布局为`LauncherRootView`，内部承载应用列表的组件的最终实现是`RecyclerView`(其外层封装了很多自定义组件)。

用户点击`Launcher`图标后，最终会调用`startAppShortcutOrInfoActivity`方法，该方法在 [`Launcher`](https://www.androidos.net.cn/android/8.0.0_r4/xref/packages/apps/Launcher3/src/com/android/launcher3/Launcher.java) 中的源码如下：

```java
private void startAppShortcutOrInfoActivity(View v) {
  ItemInfo item = (ItemInfo) v.getTag();
  Intent intent = item.getIntent();
  if (intent == null) {
    throw new IllegalArgumentException("Input must have a valid intent");
  }
  boolean success = startActivitySafely(v, intent, item); //启动Activity
  getUserEventDispatcher().logAppLaunch(v, intent); // TODO for discovered apps b/35802115

  if (success && v instanceof BubbleTextView) {
    mWaitingForResume = (BubbleTextView) v;
    mWaitingForResume.setStayPressed(true);
  }
}
```
随后调用`startActivitySafely`方法：

```java
public boolean startActivitySafely(View v, Intent intent, ItemInfo item) {
  if (mIsSafeModeEnabled && !Utilities.isSystemApp(this, intent)) {
    Toast.makeText(this, R.string.safemode_shortcut_error, Toast.LENGTH_SHORT).show();
    return false;
  }
  // Only launch using the new animation if the shortcut has not opted out (this is a
  // private contract between launcher and may be ignored in the future).
  boolean useLaunchAnimation = (v != null) &&
    !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);
  Bundle optsBundle = useLaunchAnimation ? getActivityLaunchOptions(v) : null;

  UserHandle user = item == null ? null : item.user;

  // Prepare intent
  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //注意这里要开新的任务栈
  if (v != null) {
    intent.setSourceBounds(getViewBounds(v));
  }
  try {
    if (Utilities.ATLEAST_MARSHMALLOW
        && (item instanceof ShortcutInfo)
        && (item.itemType == Favorites.ITEM_TYPE_SHORTCUT
            || item.itemType == Favorites.ITEM_TYPE_DEEP_SHORTCUT)
        && !((ShortcutInfo) item).isPromise()) {
      // Shortcuts need some special checks due to legacy reasons.
      startShortcutIntentSafely(intent, optsBundle, item);
    } else if (user == null || user.equals(Process.myUserHandle())) {
      // Could be launching some bookkeeping activity
      startActivity(intent, optsBundle);  //调用父类的方法启动Activity
    } else {
      LauncherAppsCompat.getInstance(this).startActivityForProfile(
        intent.getComponent(), user, intent.getSourceBounds(), optsBundle);
    }
    return true;
  } catch (ActivityNotFoundException|SecurityException e) {
    Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
    Log.e(TAG, "Unable to launch. tag=" + item + " intent=" + intent, e);
  }
  return false;
}
```

该方法会调用`Activity`的`startActivity`方法。

至此，从`Launcher`启动应用的本质演变为从一个`Activity`启动另一个`Activity`的流程，"`AMS`之`Activity`跨进程跳转时序图"如下图所示：

![image](https://github.com/tianyalu/NeAppLauncherProcess/raw/master/show/activity_launching_sequence_across_processes.png)

`Launcher`启动应用的主要跨进程通讯流程可以概括如下图：

![image](https://github.com/tianyalu/NeAppLauncherProcess/raw/master/show/launcher_start_app_flow_across_processes.png)

























