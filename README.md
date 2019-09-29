## FCAnnotation

本项目主要是基于注解来实现防抖和延迟等操作，类似于js的`Debounce`和`Throttle`, 并根据Android的实际业务情况加以扩展


### maven 地址

```
maven {
    url  "https://dl.bintray.com/403462630/maven"
}

```

### gradle 依赖

```
implementation 'fc.annotation:core:0.0.1'
```

### 添加Aspectj环境和依赖

本项目是依赖`Aspectj`，所以必须添加`Aspectj`的依赖

这里使用的是`aspectjx`, 因为原始的`aspectj`对kotlin支持不友好，需要配置很多东西

1. 在项目的build.gradle 中添加 classpath

	```
	classpath 'com.hujiang.aspectjx:gradle-android-plugin-aspectjx:2.0.4'
	```

2. 在app 或 library 的 build.gradle中添加插件

	```
	apply plugin: 'android-aspectjx'

	// aspectjx 非必需，如果添加了，就一定要把androidx或support包下的FragmentActivity和Fragment include进来
	aspectjx {
		// 你自己代码的packagename
		include ...
		// androidx 包需要include的类
		include 'androidx.fragment.app.FragmentActivity'
		include 'androidx.fragment.app.Fragment'
		// support 包需要include的类
		include 'android.support.v4.app.FragmentActivity'
		include 'android.support.v4.app.Fragment'
	}
	```


### API

##### @Debounce

`@Debounce` 的作用跟js的`@Debounce`一样，表示同一方法多次调用的间隔时间大于等于500毫秒（默认0，可设置），否则500毫秒之内重复调用此方法，则不会执行此方法；

注意：500毫秒之内重复调用此方法 会导致重新开始计时，这是跟`@Throttle`唯一的区别

```
@Debounce(500)
private fun testDebounce(index: Int) {
    tv_debounce.text = "debounce: $index"
}

// 子线程执行
@Debounce(500, threadModel = ATMode.ASYNC)
private fun testDebounce1(index: Int) {
    tv_debounce.text = "debounce: $index"
}

```

##### @Throttle

`@Throttle ` 的作用也跟js的`@Throttle `一样，表示同一方法多次调用的频率大于等于500毫秒（默认0，可设置），否则500毫秒之内重复调用此方法，则不会执行此方法；


```
@Throttle(500)
private fun testThrottle(index: Int) {
    tv_throttle.text = "debounce: $index"
}

// 子线程执行
@Throttle(500, threadModel = ATMode.ASYNC)
private fun testThrottle1(index: Int) {
    tv_throttle.text = "debounce: $index"
}
```

##### @Delay

`@Delay`是js里没有的一个功能，表示当执行一个方法时 延迟多少毫秒执行，跟`Android` 的 `Handler postDelay` 功能差不多

```
@Delay(
	 id = "aaa", // 表示方法唯一id，用于cancel
    value = 1000, // 延迟1000毫秒执行
    threadModel = ATMode.MAIN, // 主线执行
    isFirstDelay = false, // 第一次调用此方法 不延迟执行，之后调用再延迟执行
    isUpdateArgs = true, // 当在1000毫秒之内重复调用此方法，则更新参数为最后调用此方法的参数
    isSingleMode = true // 表示多个@Delay注解的方法是否单独延迟计时(单独执行)，还是一起延迟计时（一起执行）
)
private fun testDelay(index: Int) {
    tv_delay.text = "debounce: $index"
}

```

取消@Delay方法的执行

```
// 取消某个指定的delay方法
ATMethodManager.getInstance().cancelDelayMethod(this, "aaa")
// 如果没有定义id，则可以根据方法名取消某个指定的delay方法
ATMethodManager.getInstance().cancelDelayMethod(this, "testDelay")

// 取消所有的delay方法
ATMethodManager.getInstance().cancelAllDelayMethod(this)
```

注意：

- 所有activity在onDestroy时，会自动cancel调所有@Delay的方法
- 所有fragment在onDestroyView时，会自动cancel调所有@Delay的方法
- 其它class里如果使用了`@Delay`注解，为了防止内存泄露，需要手动canel（跟handler原理一样）
- 使用`@Debounce` `@Throttle` `@Delay`注解的方法强烈建议用`final`修饰
- `@Debounce` `@Throttle` `@Delay`注解不能在同一个方法上使用
- 方法重载 不支持 参数个数相同 同时 参数类型有继承关系; 比如：

	```
	 @Debounce(500)
    public final void test(Object obj) {

    }

    // String 是 Object子类，所以这样使用注解可能会有问题
    @Debounce(500)
    public final void test(String obj) {

    }
	```


## 混淆

```
-keepattributes *Annotation*
-keepclassmembers class ** {
    @com.fc.annotation.** <methods>;
}
```

