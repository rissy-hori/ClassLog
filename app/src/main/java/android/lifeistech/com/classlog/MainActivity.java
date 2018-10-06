package android.lifeistech.com.classlog;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    static final int REQUEST_PERMISSION = 0;
    static final int REQUEST_CODE_CAMERA = 1;

    public Realm realm;

    private boolean firstTime; // 初回起動時の処理

    private View clicked_button;
    private Button[][] buttons; // 科目別ボタン
    private TextView[] days, times; // 時間割の目盛
    private LinearLayout[] lines; // 時間割の行

    private int[][] ids; // ボタンのレイアウトidを格納
    private int[] d_ids, t_ids; // 目盛のレイアウトidを格納


    private Uri uri; // 撮影する写真の保存先

    private ImageDataListContainer mSchedule; // アプリ起動時に表示される時間割(を持つコンテナ) 。　-> アルバム/写真の新規作成時に紐づける

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        realm = Realm.getDefaultInstance();

        setButtons(); // 科目別ボタンに2種のリスナーをセット
        setBars();
        setLines();
        setClassSchedule(); // 起動時に表示する時間割を指定
        editClassScheduleView(); // Realmに保存された情報を時間割へ反映
        editSubjectView(); // Realmの中の科目情報をViewに反映

    }

    /* 未実装 */
    // 右上の設定タブを押したら新たにImageDetaListContainerのインスタンスが作成され、
    // いままで表示されていたmScheduleの isSelectedをfalseにし、
    // 新しいやつをtrueにさせて画面に反映させる

    public void setButtons() {
        ids = new int[][]{{R.id.sbj00, R.id.sbj01, R.id.sbj02, R.id.sbj03, R.id.sbj04, R.id.sbj05, R.id.sbj06},
                {R.id.sbj10, R.id.sbj11, R.id.sbj12, R.id.sbj13, R.id.sbj14, R.id.sbj15, R.id.sbj16},
                {R.id.sbj20, R.id.sbj21, R.id.sbj22, R.id.sbj23, R.id.sbj24, R.id.sbj25, R.id.sbj26},
                {R.id.sbj30, R.id.sbj31, R.id.sbj32, R.id.sbj33, R.id.sbj34, R.id.sbj35, R.id.sbj36},
                {R.id.sbj40, R.id.sbj41, R.id.sbj42, R.id.sbj43, R.id.sbj44, R.id.sbj45, R.id.sbj46},
                {R.id.sbj50, R.id.sbj51, R.id.sbj52, R.id.sbj53, R.id.sbj54, R.id.sbj55, R.id.sbj56},
                {R.id.sbj60, R.id.sbj61, R.id.sbj62, R.id.sbj63, R.id.sbj64, R.id.sbj65, R.id.sbj66},
                {R.id.sbj70, R.id.sbj71, R.id.sbj72, R.id.sbj73, R.id.sbj74, R.id.sbj75, R.id.sbj76},
                {R.id.sbj80, R.id.sbj81, R.id.sbj82, R.id.sbj83, R.id.sbj84, R.id.sbj85, R.id.sbj86},
                {R.id.sbj90, R.id.sbj91, R.id.sbj92, R.id.sbj93, R.id.sbj94, R.id.sbj95, R.id.sbj96},
        };
        // データ型[][] 配列名 = new データ型名[Ｙ方向の長さ][Ｘ方向の長さ];
        buttons = new Button[10][7]; // 月〜日、1限〜10限
        for (int i = 0; i < ids.length; i++) {
            for (int j = 0; j < ids[0].length; j++) {
                buttons[i][j] = findViewById(ids[i][j]);
                buttons[i][j].setOnClickListener(this); // タップを探知
                buttons[i][j].setOnLongClickListener(this); // 長押しを探知
            }
        }
    }

    public void setBars() {
        d_ids = new int[]{R.id.Mon, R.id.Tue, R.id.Wed, R.id.Thu, R.id.Fri, R.id.Sat, R.id.Sun};
        t_ids = new int[]{R.id.t1, R.id.t2, R.id.t3, R.id.t4, R.id.t5, R.id.t6, R.id.t7, R.id.t8, R.id.t9, R.id.t10};

        days = new TextView[7];
        times = new TextView[10];
        for (int i = 0; i < d_ids.length; i++) {
            days[i] = findViewById(d_ids[i]);
        }
        for (int i = 0; i < t_ids.length; i++) {
            times[i] = findViewById(t_ids[i]);
        }
    }

    public void setLines() {
        int[] l_ids = new int[]{R.id.L1, R.id.L2, R.id.L3, R.id.L4, R.id.L5, R.id.L6, R.id.L7, R.id.L8, R.id.L9, R.id.L10};
        lines = new LinearLayout[10];
        for (int i = 0; i < l_ids.length; i++) {
            lines[i] = findViewById(l_ids[i]);
        }
    }

    public void setClassSchedule() {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {

                mSchedule = realm.where(ImageDataListContainer.class).equalTo("isSelected",
                        true).findFirst(); // アプリ起動時に表示する時間割を取得

                if (mSchedule == null) { // アプリ初回起動時
                    // 以下の処理を今後メソッド化して、時間割追加作成時にも使用する

                    mSchedule = new ImageDataListContainer();   // 時間割新規作成
                    mSchedule.setTimestamp(makeTimestamp());
                    RealmList<ImageDataList> newSchedule = new RealmList<>();
                    mSchedule.setClassSchedule(newSchedule); // 講義情報未登録のリスト

                    RealmList<ImageDataList> lists = mSchedule.getClassSchedule();

                    /* 科目表示を動的に変更できるかチェック */
                    ImageDataList sampleAlbum = new ImageDataList();
                    sampleAlbum.setPosition(R.id.sbj11);
                    sampleAlbum.setName("sample1");

                    ImageDataList sampleAlbum2 = new ImageDataList();
                    sampleAlbum2.setPosition(R.id.sbj22);
                    sampleAlbum2.setName("sample2");

                    lists.add(sampleAlbum);
                    lists.add(sampleAlbum2);



                    mSchedule.setSelected(true); // これを

                    // 月〜金、1限〜5限 を持つ、という情報を保存
                    mSchedule.setHasSaturday(true);
                    mSchedule.setHasSunday(true);
                    mSchedule.setHasT1(true);
                    mSchedule.setHasT2(true);
                    mSchedule.setHasT3(true);
                    mSchedule.setHasT4(true);
                    mSchedule.setHasT5(true);
                    mSchedule.setHasT6(true);
                    mSchedule.setHasT7(true);
                    mSchedule.setHasT8(false);
                    mSchedule.setHasT9(false);
                    mSchedule.setHasT10(false);
                }
                realm.copyToRealm(mSchedule); // これから反映させる時間割の情報をRealmに保存
            }
        });
    }

    public void editClassScheduleView() { // 月~金 or 月~土 or 月~日, ~5or6or7限 のみ実装済み
        //　デフォルト(月〜金、1~5限)と異なる場合は、ボタンのvisiblityを変更する
        //　ただし、6限がなくて7限がある、という状況は避ける。

        if (mSchedule.hasSaturday()) { // 1~5限まで土曜日を出現
            days[5].setVisibility(View.VISIBLE);
            for (int i = 0; i < 5; i++) {
                buttons[i][5].setVisibility(View.VISIBLE);
            }
            if (mSchedule.hasSaturday()) { // 1~5限まで日曜日を出現
                days[6].setVisibility(View.VISIBLE);
                for (int i = 0; i < 5; i++) {
                    buttons[i][6].setVisibility(View.VISIBLE);
                }
            }
        }
        if (mSchedule.hasT6()) { // 6限目を出現
            lines[5].setVisibility(View.VISIBLE);
            times[5].setVisibility(View.VISIBLE);
            for (int i = 0; i < 5; i++) {
                buttons[5][i].setVisibility(View.VISIBLE);
            }
            if (mSchedule.hasSaturday()) {
                buttons[5][5].setVisibility((View.VISIBLE)); // 土曜6限を出現
                if (mSchedule.hasSaturday()) {
                    buttons[5][6].setVisibility(View.VISIBLE); // 土曜7限を出現
                }
            }
        }
        if (mSchedule.hasT7()) {
            lines[6].setVisibility(View.VISIBLE);
            times[6].setVisibility(View.VISIBLE);
            for (int i = 0; i < 5; i++) {
                buttons[6][i].setVisibility(View.VISIBLE);
            }
            if (mSchedule.hasSaturday()) {
                buttons[6][5].setVisibility((View.VISIBLE)); // 日曜6限を出現
                if (mSchedule.hasSaturday()) {
                    buttons[6][6].setVisibility(View.VISIBLE); // 日曜7限を出現
                }
            }
        }
    }

    public void editSubjectView() {

        RealmList<ImageDataList> lists = mSchedule.getClassSchedule(); // 科目情報リストを取得

        if (!lists.isEmpty()) { // 科目情報が少なくとも1つ登録されているとき
            for (ImageDataList album : lists) {
                int n = album.getPosition();
                Button button = null;

                loop:
                for (int i = 0; i < ids.length; i++) {
                    for (int j = 0; j < ids[0].length; j++) {

                        if (n == ids[i][j]) {
                            button = buttons[i][j];
                            break loop; // 目当ての科目を見つけたら二重for文を抜ける
                        }
                    }
                }

                button.setText(album.getName());
                // 色の変更もここに書く
            }
        }
    }


    // mScheduleの持つリストからImageDataListを取り出し、
    // それの持つ情報をカスタムしたImageViewに反映させる


    // タイムスタンプ作成
    public final String makeTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss"); // 日時データを元に文字列を形成
        final String nowStr = dateFormat.format(new Date(System.currentTimeMillis()));

        return nowStr;
    }


    /* タップ： ギャラリー(アルバム)へ移動*/
    @Override
    public void onClick(View v) {

        // アルバムに時間割が紐づいていなければ、ここでmScheduleに紐づける


        Intent to_gallery = new Intent(this, DetailActivity.class);


        //もし科目名などが登録されていれば、Intent.extraで渡して、DetailActivityのバーに表示

        to_gallery.putExtra("imageUri", uri.toString());
        // 科目情報もIntentに持たせる


        startActivity(to_gallery); // 科目別アルバム(DetailActivity)へ飛ぶ
    }


    /* 長押し：カメラの起動 */
    @Override
    public boolean onLongClick(final View view) {

        // WRITE_EXTERNAL_IMAGE が未許可の場合
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION); //WRITE_EXTERNAL_STORAGEの許可を求めるダイアログを表示

            clicked_button = view; // Clickされた科目に対応するviewをクラスフィールドに保存

            return true;  // 戻り値をtrueにするとOnClickイベントは発生しない(falseだと最後にonClickイベント発生)
        }

        final String nowStr = makeTimestamp(); // カメラ起動時の日時取得

        String fileName = "ClassLog_" + nowStr + ".jpg"; // 保存する画像のファイル名を生成

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName); // 画像ファイル名を設定
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg"); // 画像ファイルの種類を設定

        ContentResolver resolver = getContentResolver();
        uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values); // URI作成

        Log.d("view.getID", String.valueOf(view.getId()));


        // カメラ起動　ー＞　撮影　ー＞　保存（ ー＞連続撮影 ）　ー＞　科目別アルバム(DetailActivity)へ

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                /* イメージの作成 (撮影前に、すべての情報を保存)   -> キャンセルされたときの処理も追加　or DetailActivityでこの処理を行う    */
                ImageData image = realm.createObject(ImageData.class);
                image.setTimestamp(nowStr);
                image.setUri(uri.toString());
                image.setSubject(view.getId() + "");
                Log.d("subject", image.getSubject());

                image.setName(((Button) view).getText() + "_" + nowStr);
                realm.copyToRealm(image);
            }
        });


        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, REQUEST_CODE_CAMERA);


        return true;    // 戻り値をtrueにするとOnClickイベントは発生しない(falseだと最後にonClickイベント発生)
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 再度カメラアプリを起動
            onLongClick(clicked_button);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CAMERA) {
                //Bitmap bmp = (Bitmap) intent.getExtras().get("data");
                //imageView.setImageBitmap(bmp);

                Toast.makeText(MainActivity.this, "画像を保存しました", Toast.LENGTH_LONG).show();

                Intent intent_main = new Intent(this, DetailActivity.class);
                intent_main.putExtra("imageUri", uri.toString());
                // 科目情報もIntentに持たせる
                startActivity(intent_main); // 科目別アルバム(DetailActivity)へ飛ぶ
            }
        } else if (resultCode == RESULT_CANCELED) {
            // キャンセルされたらToastを表示
            Toast.makeText(MainActivity.this, "CANCELED", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //realmを閉じる
        realm.close();
    }
}

