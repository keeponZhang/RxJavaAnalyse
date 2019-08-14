package com.example.atomiclearn;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.atomiclearn.bean.Person;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AutomicActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        atomicReferenceTest();
        atomicBoolean();
    }
    private void atomicBoolean() {
        //可以给AtomicBoolean传递一个初始值
        AtomicBoolean atomicBoolean = new AtomicBoolean(true);
        atomicBoolean.set(false);
        boolean value = atomicBoolean.get();
        Log.e("TAG", "AutomicActivity atomicBoolean:" + value);

        boolean expectedValue = true;
        boolean newValue      = false;

        boolean wasNewValueSet = atomicBoolean.compareAndSet(
                expectedValue, newValue);
        boolean wasNewValueSet2 = atomicBoolean.compareAndSet(
                false, true);
        Log.e("TAG", "AutomicActivity atomicBoolean wasNewValueSet:"+wasNewValueSet+"  wasNewValueSet2="+wasNewValueSet2);
    }
    private void atomicReferenceTest() {
        // 创建两个Person对象，它们的id分别是101和102。
        Person p1 = new Person(101);
        Person p2 = new Person(102);
        // 新建AtomicReference对象，初始化它的值为p1对象
        AtomicReference ar = new AtomicReference(p1);
        // 通过CAS设置ar。如果ar的值为p1的话，则将其设置为p2。
        ar.compareAndSet(p1, p2);

        Person p3 = (Person)ar.get();
        Log.e("TAG", "AutomicActivity atomicReferenceTest p3 is "+p3);
        Log.e("TAG", "AutomicActivity atomicReferenceTest p3.equals(p1)="+p3.equals(p1));
        Log.e("TAG", "AutomicActivity atomicReferenceTest p3.equals(p2)="+p3.equals(p2));
        AtomicReference atomicReference = new AtomicReference();
        //tomicReference.get():null
        Log.e("TAG", "AutomicActivity atomicReferenceTest atomicReference.get():" + atomicReference.get());
    }
}
