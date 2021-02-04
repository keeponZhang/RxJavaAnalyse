package zlc.season.rxjava2demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.io.InterruptedIOException;

import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import zlc.season.rxjava2demo.demo.ChapterEight;
import zlc.season.rxjava2demo.demo.ChapterFive;
import zlc.season.rxjava2demo.demo.ChapterFour;
import zlc.season.rxjava2demo.demo.ChapterNine;
import zlc.season.rxjava2demo.demo.ChapterOne;
import zlc.season.rxjava2demo.demo.ChapterSeven;
import zlc.season.rxjava2demo.demo.ChapterThree;
import zlc.season.rxjava2demo.demo.ChapterTwo;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "TAG";

    static {
        // RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
        //     @Override
        //     public void accept(Throwable throwable) throws Exception {
        //         if (throwable instanceof InterruptedIOException) {
        //             Log.d(TAG, "Io interrupted");
        //         }
        //         Log.e("TAG", "MainActivity accept Throwable:");
        //     }
        // });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // testChapter1();
                testChapter2();
               // testChapter3();
               //                 testChapter4();
                //              testChapter5();
//                testChapter6();
                //  testChapter7();
//                testChapter8();
//                testChapter9();

            }
        });

        findViewById(R.id.request).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //95 上游不会再发送事件
//                ChapterNine.request(95);
                ChapterNine.request(96);
            }
        });
    }

    private void testChapter1() {
        ChapterOne.demo1();
//        ChapterOne.demo2();
//        ChapterOne.demo3();
//        ChapterOne.demo4();
    }

    private void testChapter2() {
//        ChapterTwo.demo1();
//        ChapterTwo.demo2();
//        ChapterTwo.demo3();
        ChapterTwo.demo4();
    }

    private void testChapter3() {
//       ChapterThree.demo1();
        ChapterThree.demo2();
//        ChapterThree.demo3();
    }

    private void testChapter4() {
          ChapterFour.demo1();
//        ChapterFour.demo2();
//         ChapterFour.demo3();
    }

    private void testChapter5() {
//      ChapterFive.demo1();
        //ChapterFive.demo2();
        ChapterFive.demo3();
    }

    private void testChapter6() {
//       ChapterSix.demo1();
        //  ChapterSix.demo2();
//           ChapterSix.demo3();
//        ChapterSix.demo4();
//           ChapterSix.demo5();
//           ChapterSix.demo6();
    }

    private void testChapter7() {
//        ChapterSeven.demo1();
//        ChapterSeven.demo2();
//               ChapterSeven.demo3();
//              ChapterSeven.demo4();
        ChapterSeven.demo5();
    }

    private void testChapter8() {
        // ChapterEight.demo1();
        // ChapterEight.demo2();
        //    ChapterEight.demo3();
//             ChapterEight.demo4();
        //       ChapterEight.demo5();
               ChapterEight.demo6();
        //       ChapterEight.demo7();
    }
    private void testChapter9() {
        //      ChapterNine.demo1();
        //   ChapterNine.demo2();
//              ChapterNine.demo3();
            ChapterNine.demo4();
    }

}
