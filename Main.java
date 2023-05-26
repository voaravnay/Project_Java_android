package com.yanvaryov.tictac;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main extends Activity {
    Toast toast;
    final float COMP_LEVEL = 0.5f; // computer thinking level form 0 to 1

    SharedPreferences sp;
    boolean player1;
    int num_pressed;
    boolean isForeground = true;
    int[] cells_values = new int[15];
    List<ImageView> cells = new ArrayList<ImageView>();
    AnimatorSet anim;
    Handler h = new Handler();
    boolean single_game;
    boolean comp_first;
    SoundPool sndpool;
    int snd_click;
    int snd_win;
    int screen_width;
    int screen_height;
    MediaPlayer mp = new MediaPlayer();
    int num_games = 0;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // preferences
        sp = PreferenceManager.getDefaultSharedPreferences(this);



        show_main.run();

        // bg sound
        try {
            mp = new MediaPlayer();
            AssetFileDescriptor descriptor = getAssets().openFd("snd_bg.mp3");
            mp.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setVolume(0, 0);
            mp.setLooping(true);
            mp.prepare();
            mp.start();
        } catch (Exception e) {
        }

        // if mute
        if (sp.getBoolean("mute", false))
            ((Button) findViewById(R.id.btn_sound)).setText(getString(R.string.btn_sound));
        else
            mp.setVolume(0.5f, 0.5f);

        // SoundPool
        sndpool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        try {
            snd_click = sndpool.load(getAssets().openFd("snd_click.mp3"), 1);
            snd_win = sndpool.load(getAssets().openFd("snd_win.mp3"), 1);
        } catch (IOException e) {
        }

        // hide navigation bar listener
        findViewById(R.id.all).setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                hide_navigation_bar();
            }
        });

        // add cells
        for (int i = 0; i < 9; i++) {
            ImageView cell = new ImageView(this);
            cell.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            cell.setTag(i);
            cell.setClickable(true);
            ((ViewGroup) findViewById(R.id.frame)).addView(cell);
            cells.add(cell);

            // touch listener
            cell.setOnTouchListener(new OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // cell click
                    if (event.getAction() == MotionEvent.ACTION_DOWN && v.isEnabled()
                            && ((single_game && !player1) || !single_game))
                        CELL_CLICK(v);
                    return false;
                }
            });
        }

        // custom font
        Typeface font = Typeface.createFromAsset(getAssets(), "CooperBlack.otf");
        ((TextView) findViewById(R.id.mess)).setTypeface(font);

        SCALE();
    }

    // SCALE
    void SCALE() {
        // text
        ((TextView) findViewById(R.id.mess)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(26));
        ((TextView) findViewById(R.id.txt_score)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(26));

        // buttons text
        ((TextView) findViewById(R.id.btn_clear)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(30));
        ((TextView) findViewById(R.id.btn_sound)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(30));
        ((TextView) findViewById(R.id.btn_start1)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(26));
        ((TextView) findViewById(R.id.btn_start2)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(26));
        ((TextView) findViewById(R.id.btn_exit)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(40));
    }

    // START
    void START() {
        player1 = true;
        num_pressed = 0;
        findViewById(R.id.game).setVisibility(View.VISIBLE);
        findViewById(R.id.main).setVisibility(View.GONE);
        findViewById(R.id.mess).setVisibility(View.GONE);

        // screen size
        screen_width = findViewById(R.id.all).getWidth();
        screen_height = findViewById(R.id.all).getHeight();

        // cell size
        int cell_size = (int) ((screen_width - DpToPx(20)) / 3);

        // frame size and position
        findViewById(R.id.frame).getLayoutParams().width = cell_size * 3;
        findViewById(R.id.frame).getLayoutParams().height = cell_size * 3;

        // cells at start
        int x_pos = 0;
        int y_pos = 0;
        for (int i = 0; i < cells.size(); i++) {
            cells_values[i] = 0;
            cells.get(i).setEnabled(true);
            cells.get(i).setImageResource(0);
            cells.get(i).getLayoutParams().width = cell_size;
            cells.get(i).getLayoutParams().height = cell_size;
            cells.get(i).setX(x_pos * cell_size);
            cells.get(i).setY(y_pos * cell_size);
            cells.get(i).setScaleX(1);
            cells.get(i).setScaleY(1);

            x_pos++;
            if (x_pos == 3) {
                x_pos = 0;
                y_pos++;
            }
        }

        // computer go
        if (single_game)
            h.postDelayed(computer_go, 500);
    }

    // onClick
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start1:
                // single player game
                single_game = true;
                comp_first = true;
                START();
                break;
            case R.id.btn_start2:
                // multiple player game
                single_game = false;
                START();
                break;
            case R.id.btn_clear:
                // clear scores
                sp.edit().remove("player1").remove("player2").apply();
                show_main.run();
                break;
            case R.id.btn_exit:
                finish();
                break;
            case R.id.btn_sound:
                // sound
                if (sp.getBoolean("mute", false)) {
                    sp.edit().putBoolean("mute", false).apply();
                    mp.setVolume(0.5f, 0.5f);
                    ((Button) findViewById(R.id.btn_sound)).setText(getString(R.string.btn_mute));
                } else {
                    sp.edit().putBoolean("mute", true).apply();
                    mp.setVolume(0, 0);
                    ((Button) findViewById(R.id.btn_sound)).setText(getString(R.string.btn_sound));
                }
                break;
        }
    }

    // CELL_CLICK
    void CELL_CLICK(View v) {
        v.setEnabled(false);

        // sound
        if (!sp.getBoolean("mute", false) && isForeground)
            sndpool.play(snd_click, 0.4f, 0.4f, 0, 0, 1);

        // set cell icon
        if (player1) {
            ((ImageView) v).setImageResource(R.drawable.player1);
            cells_values[Integer.valueOf(v.getTag().toString())] = 1;
        } else {
            ((ImageView) v).setImageResource(R.drawable.player2);
            cells_values[Integer.valueOf(v.getTag().toString())] = 2;
        }
        player1 = !player1;
        num_pressed++;

        // check winner
        if (check_winner(1)) {
            sp.edit().putInt("player1", sp.getInt("player1", 0) + 1).apply();
            STOP(1);
            return;
        }
        if (check_winner(2)) {
            sp.edit().putInt("player2", sp.getInt("player2", 0) + 1).apply();
            STOP(2);
            return;
        }

        // no winner
        if (num_pressed == cells.size()) {
            STOP(0);
            return;
        }

        // computer go
        if (single_game && player1)
            h.postDelayed(computer_go, 500);
    }

    // best_go
    int best_go() {
        // left only one step to win
        if (cells_values[0] == 0 && cells_values[1] == 1 && cells_values[2] == 1)
            return 0;
        else if (cells_values[1] == 0 && cells_values[0] == 1 && cells_values[2] == 1)
            return 1;
        else if (cells_values[2] == 0 && cells_values[0] == 1 && cells_values[1] == 1)
            return 2;
        else if (cells_values[3] == 0 && cells_values[4] == 1 && cells_values[5] == 1)
            return 3;
        else if (cells_values[4] == 0 && cells_values[3] == 1 && cells_values[5] == 1)
            return 4;
        else if (cells_values[5] == 0 && cells_values[3] == 1 && cells_values[4] == 1)
            return 5;
        else if (cells_values[6] == 0 && cells_values[7] == 1 && cells_values[8] == 1)
            return 6;
        else if (cells_values[7] == 0 && cells_values[6] == 1 && cells_values[8] == 1)
            return 7;
        else if (cells_values[8] == 0 && cells_values[6] == 1 && cells_values[7] == 1)
            return 8;
        else if (cells_values[0] == 0 && cells_values[3] == 1 && cells_values[6] == 1)
            return 0;
        else if (cells_values[3] == 0 && cells_values[0] == 1 && cells_values[6] == 1)
            return 3;
        else if (cells_values[6] == 0 && cells_values[0] == 1 && cells_values[3] == 1)
            return 6;
        else if (cells_values[1] == 0 && cells_values[4] == 1 && cells_values[7] == 1)
            return 1;
        else if (cells_values[4] == 0 && cells_values[1] == 1 && cells_values[7] == 1)
            return 4;
        else if (cells_values[7] == 0 && cells_values[1] == 1 && cells_values[4] == 1)
            return 7;
        else if (cells_values[2] == 0 && cells_values[5] == 1 && cells_values[8] == 1)
            return 2;
        else if (cells_values[5] == 0 && cells_values[2] == 1 && cells_values[8] == 1)
            return 5;
        else if (cells_values[8] == 0 && cells_values[2] == 1 && cells_values[5] == 1)
            return 8;
        else if (cells_values[0] == 0 && cells_values[4] == 1 && cells_values[8] == 1)
            return 0;
        else if (cells_values[4] == 0 && cells_values[0] == 1 && cells_values[8] == 1)
            return 4;
        else if (cells_values[8] == 0 && cells_values[0] == 1 && cells_values[4] == 1)
            return 8;
        else if (cells_values[2] == 0 && cells_values[4] == 1 && cells_values[6] == 1)
            return 2;
        else if (cells_values[4] == 0 && cells_values[2] == 1 && cells_values[6] == 1)
            return 4;
        else if (cells_values[6] == 0 && cells_values[2] == 1 && cells_values[4] == 1)
            return 6;

        // prevent you win
        if (Math.random() > COMP_LEVEL) {
            if (cells_values[0] == 0 && ((cells_values[1] == 2 && cells_values[2] == 2) || (cells_values[3] == 2 && cells_values[6] == 2) || (cells_values[4] == 2 && cells_values[8] == 2)))
                return 0;
            else if (cells_values[1] == 0 && ((cells_values[0] == 2 && cells_values[2] == 2) || (cells_values[4] == 2 && cells_values[7] == 2)))
                return 1;
            else if (cells_values[2] == 0 && ((cells_values[0] == 2 && cells_values[1] == 2) || (cells_values[5] == 2 && cells_values[8] == 2) || (cells_values[4] == 2 && cells_values[6] == 2)))
                return 2;
            else if (cells_values[3] == 0 && ((cells_values[0] == 2 && cells_values[6] == 2) || (cells_values[4] == 2 && cells_values[5] == 2)))
                return 3;
            else if (cells_values[4] == 0 && ((cells_values[0] == 2 && cells_values[8] == 2) || (cells_values[1] == 2 && cells_values[7] == 2) || (cells_values[2] == 2 && cells_values[6] == 2) || (cells_values[3] == 2 && cells_values[5] == 2)))
                return 4;
            else if (cells_values[5] == 0 && ((cells_values[2] == 2 && cells_values[8] == 2) || (cells_values[3] == 2 && cells_values[4] == 2)))
                return 5;
            else if (cells_values[6] == 0 && ((cells_values[0] == 2 && cells_values[3] == 2) || (cells_values[2] == 2 && cells_values[4] == 2) || (cells_values[7] == 2 && cells_values[8] == 2)))
                return 6;
            else if (cells_values[7] == 0 && ((cells_values[1] == 2 && cells_values[4] == 2) || (cells_values[6] == 2 && cells_values[8] == 2)))
                return 7;
            else if (cells_values[8] == 0 && ((cells_values[0] == 2 && cells_values[4] == 2) || (cells_values[2] == 2 && cells_values[5] == 2) || (cells_values[6] == 2 && cells_values[7] == 2)))
                return 8;
        }

        // only one cell from three is checked
        if (cells_values[0] == 1 && cells_values[4] == 0 && cells_values[8] != 2)
            return 4;
        else if (cells_values[0] == 1 && cells_values[1] == 0 && cells_values[2] != 2)
            return 1;
        else if (cells_values[0] == 1 && cells_values[3] == 0 && cells_values[6] != 2)
            return 3;
        else if (cells_values[1] == 1 && cells_values[4] == 0 && cells_values[7] != 2)
            return 4;
        else if (cells_values[1] == 1 && cells_values[2] == 0 && cells_values[0] != 2)
            return 2;
        else if (cells_values[1] == 1 && cells_values[0] == 0 && cells_values[2] != 2)
            return 0;
        else if (cells_values[2] == 1 && cells_values[4] == 0 && cells_values[6] != 2)
            return 4;
        else if (cells_values[2] == 1 && cells_values[1] == 0 && cells_values[0] != 2)
            return 1;
        else if (cells_values[2] == 1 && cells_values[5] == 0 && cells_values[8] != 2)
            return 5;
        else if (cells_values[3] == 1 && cells_values[4] == 0 && cells_values[5] != 2)
            return 4;
        else if (cells_values[3] == 1 && cells_values[0] == 0 && cells_values[6] != 2)
            return 0;
        else if (cells_values[3] == 1 && cells_values[6] == 0 && cells_values[0] != 2)
            return 6;
        else if (cells_values[4] == 1 && cells_values[0] == 0 && cells_values[8] != 2)
            return 0;
        else if (cells_values[4] == 1 && cells_values[8] == 0 && cells_values[0] != 2)
            return 8;
        else if (cells_values[4] == 1 && cells_values[1] == 0 && cells_values[7] != 2)
            return 1;
        else if (cells_values[4] == 1 && cells_values[7] == 0 && cells_values[1] != 2)
            return 7;
        else if (cells_values[4] == 1 && cells_values[2] == 0 && cells_values[6] != 2)
            return 2;
        else if (cells_values[4] == 1 && cells_values[6] == 0 && cells_values[2] != 2)
            return 6;
        else if (cells_values[4] == 1 && cells_values[3] == 0 && cells_values[5] != 2)
            return 3;
        else if (cells_values[4] == 1 && cells_values[5] == 0 && cells_values[3] != 2)
            return 5;
        else if (cells_values[5] == 1 && cells_values[4] == 0 && cells_values[3] != 2)
            return 4;
        else if (cells_values[5] == 1 && cells_values[2] == 0 && cells_values[8] != 2)
            return 2;
        else if (cells_values[5] == 1 && cells_values[8] == 0 && cells_values[2] != 2)
            return 8;
        else if (cells_values[6] == 1 && cells_values[4] == 0 && cells_values[2] != 2)
            return 4;
        else if (cells_values[6] == 1 && cells_values[3] == 0 && cells_values[0] != 2)
            return 3;
        else if (cells_values[6] == 1 && cells_values[7] == 0 && cells_values[8] != 2)
            return 7;
        else if (cells_values[7] == 1 && cells_values[4] == 0 && cells_values[1] != 2)
            return 4;
        else if (cells_values[7] == 1 && cells_values[6] == 0 && cells_values[8] != 2)
            return 6;
        else if (cells_values[7] == 1 && cells_values[8] == 0 && cells_values[6] != 2)
            return 8;
        else if (cells_values[8] == 1 && cells_values[4] == 0 && cells_values[0] != 2)
            return 4;
        else if (cells_values[8] == 1 && cells_values[5] == 0 && cells_values[2] != 2)
            return 5;
        else if (cells_values[8] == 1 && cells_values[7] == 0 && cells_values[6] != 2)
            return 7;

        // random cell
        List<Integer> free_cells = new ArrayList<Integer>();
        for (int i = 0; i < cells.size(); i++)
            if (cells_values[i] == 0)
                free_cells.add(i);
        return free_cells.get((int) Math.round(Math.random() * (free_cells.size() - 1)));
    }

    // computer_go
    Runnable computer_go = new Runnable() {
        @Override
        public void run() {
            if (comp_first && Math.random() > COMP_LEVEL)
                CELL_CLICK(cells.get(4)); // first go
            else
                CELL_CLICK(cells.get(best_go())); // best go

            comp_first = false;
        }
    };

    // show_main
    Runnable show_main = new Runnable() {
        public void run() {
            findViewById(R.id.game).setVisibility(View.GONE);
            findViewById(R.id.main).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.txt_score)).setText(sp.getInt("player1", 0) + ":" + sp.getInt("player2", 0));
        }
    };

    // STOP
    void STOP(int result) {
        num_games++;

        // disable cells
        for (int i = 0; i < cells.size(); i++)
            cells.get(i).setEnabled(false);

        // sound
        if (!sp.getBoolean("mute", false) && isForeground && (result == 1 || result == 2))
            sndpool.play(snd_win, 0.5f, 0.5f, 0, 0, 1);

        // message
        switch (result) {
            case 0:
                ((TextView) findViewById(R.id.mess)).setText(R.string.win0);
                h.postDelayed(show_main, 2000);
                break;
            case 1:
                if (single_game)
                    ((TextView) findViewById(R.id.mess)).setText(R.string.win1_single);
                else
                    ((TextView) findViewById(R.id.mess)).setText(R.string.win1_multiple);
                h.postDelayed(show_main, 3000);
                break;
            case 2:
                if (single_game)
                    ((TextView) findViewById(R.id.mess)).setText(R.string.win2_single);
                else
                    ((TextView) findViewById(R.id.mess)).setText(R.string.win2_multiple);
                h.postDelayed(show_main, 3000);
                break;
        }

        findViewById(R.id.mess).setVisibility(View.VISIBLE);
    }

    // check_winner
    boolean check_winner(int player) {
        if (cells_values[0] == player && cells_values[1] == player && cells_values[2] == player) {
            show_animation(new int[]{0, 1, 2});
            return true;
        }

        if (cells_values[3] == player && cells_values[4] == player && cells_values[5] == player) {
            show_animation(new int[]{3, 4, 5});
            return true;
        }

        if (cells_values[6] == player && cells_values[7] == player && cells_values[8] == player) {
            show_animation(new int[]{6, 7, 8});
            return true;
        }

        if (cells_values[0] == player && cells_values[3] == player && cells_values[6] == player) {
            show_animation(new int[]{0, 3, 6});
            return true;
        }

        if (cells_values[1] == player && cells_values[4] == player && cells_values[7] == player) {
            show_animation(new int[]{1, 4, 7});
            return true;
        }

        if (cells_values[2] == player && cells_values[5] == player && cells_values[8] == player) {
            show_animation(new int[]{2, 5, 8});
            return true;
        }

        if (cells_values[0] == player && cells_values[4] == player && cells_values[8] == player) {
            show_animation(new int[]{0, 4, 8});
            return true;
        }

        if (cells_values[2] == player && cells_values[4] == player && cells_values[6] == player) {
            show_animation(new int[]{2, 4, 6});
            return true;
        }

        return false;
    }

    // show_animation
    void show_animation(int[] array) {
        // AnimatorSet
        List<Animator> anim_list = new ArrayList<Animator>();

        // create animation
        for (int i = 0; i < array.length; i++) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(cells.get(array[i]), "scaleX", 0.5f);
            anim.setRepeatCount(5);
            anim.setRepeatMode(ObjectAnimator.REVERSE);
            anim_list.add(anim);

            anim = ObjectAnimator.ofFloat(cells.get(array[i]), "scaleY", 0.5f);
            anim.setRepeatCount(5);
            anim.setRepeatMode(ObjectAnimator.REVERSE);
            anim_list.add(anim);
        }

        // animate
        anim = new AnimatorSet();
        anim.playTogether(anim_list);
        anim.setDuration(200);
        anim.start();
    }

    // DpToPx
    float DpToPx(float dp) {
        return (dp * Math.max(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels) / 500f);
    }

    // hide_navigation_bar
    void hide_navigation_bar() {
        // fullscreen mode
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            hide_navigation_bar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isForeground = true;

        if (!sp.getBoolean("mute", false) && isForeground)
            mp.setVolume(0.5f, 0.5f);
    }

    @Override
    protected void onPause() {
        isForeground = false;
        mp.setVolume(0, 0);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.main).getVisibility() == View.VISIBLE)
            super.onBackPressed();
        else {
            findViewById(R.id.main).setVisibility(View.VISIBLE);
            h.removeCallbacks(show_main);
            h.removeCallbacks(computer_go);

            if (anim != null)
                anim.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacks(show_main);
        h.removeCallbacks(computer_go);
        sndpool.release();
        mp.release();

        if (anim != null)
            anim.cancel();

        super.onDestroy();
    }

}