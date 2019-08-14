# AtomicReference源码分析
```java
public class AtomicReference<V>  implements java.io.Serializable {
    private static final long serialVersionUID = -1848883965231344442L;
 
    // 获取Unsafe对象，Unsafe的作用是提供CAS操作
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long valueOffset;
 
    static {
      try {
        valueOffset = unsafe.objectFieldOffset
            (AtomicReference.class.getDeclaredField("value"));
      } catch (Exception ex) { throw new Error(ex); }
    }
 
    // volatile类型
    private volatile V value;
 
    public AtomicReference(V initialValue) {
        value = initialValue;
    }
 
    public AtomicReference() {
    }
 
    public final V get() {
        return value;
    }
 
    public final void set(V newValue) {
        value = newValue;
    }
 
    public final void lazySet(V newValue) {
        unsafe.putOrderedObject(this, valueOffset, newValue);
    }
 
    public final boolean compareAndSet(V expect, V update) {
        return unsafe.compareAndSwapObject(this, valueOffset, expect, update);
    }
 
    public final boolean weakCompareAndSet(V expect, V update) {
        return unsafe.compareAndSwapObject(this, valueOffset, expect, update);
    }
 
    public final V getAndSet(V newValue) {
        while (true) {
            V x = get();
            if (compareAndSet(x, newValue))
                return x;
        }
    }
 
    public String toString() {
        return String.valueOf(get());
    }
}
```
* AtomicReference的源码比较简单。它是通过"volatile"和"Unsafe提供的CAS函数实现"原子操作。
  * 1.value是volatile类型。这保证了：当某线程修改value的值时，其他线程看到的value值都是最新的value值，即修改之后的volatile的值。
  * 2.通过CAS设置value。这保证了：当某线程池通过CAS函数(如compareAndSet函数)设置value时，它的操作是原子的，即线程在操作value时不会被中断。