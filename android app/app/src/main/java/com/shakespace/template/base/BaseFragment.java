package com.shakespace.template.base;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * A simple {@link Fragment} subclass.
 */
public abstract class BaseFragment extends Fragment {
    private Unbinder mBinder;
    Boolean up = false;//默认false不刷新

    @Override
    public void onPause() {
        super.onPause();
        up = true;//不可见的时候将刷新开启
    }


    public BaseFragment() {
        // Required empty public constructor
    }

    //是否第一次加载
    private boolean isFirstLoading = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = initView(inflater, container, savedInstanceState);
        mBinder = ButterKnife.bind(this, view);
        initListener(view);
        return view;
    }

    /**
     *
     * @param view  根视图View  已经绑定
     */
    protected abstract void initListener(View view);

    protected abstract View initView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBinder != null) {
            mBinder.unbind();
        }
    }

    //---避免叠加
    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (getView() != null) {
            getView().setVisibility(menuVisible ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
