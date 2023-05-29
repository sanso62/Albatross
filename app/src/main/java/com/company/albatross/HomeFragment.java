package com.company.albatross;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.ListFragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends ListFragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String KEY_CURRENT_ITEM = "current_item";
    private static final String KEY_LIST_STATE = "list_state";
    private static final int SLIDESHOW_DELAY_MS = 3000;

    private static final int[] IMAGES = {R.drawable.banner1, R.drawable.banner2, R.drawable.banner3};
    private int currentItem = 0;
    private ImageView imageView;
    private Thread slideThread = null;
    private Timer timer;
    private FragmentActivity mActivity;


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    private DatabaseReference mDatabase;

    private ArrayList<String> employerIdTokens = new ArrayList<>();
    private ArrayList<String> ids = new ArrayList<>();

    private EditText editText;
    private ListAdapter adapter;
    private ArrayList<String> items;
    private ArrayList<String> filteredItems;


    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }


    public void startTimer() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (mActivity != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // update UI here
                        }
                    });
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 0, 1000);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            Parcelable listState = savedInstanceState.getParcelable(KEY_LIST_STATE);
            getListView().onRestoreInstanceState(listState);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        startSlideshow();
        //setupListView();
    }

    @Override
    public void onPause() {
        super.onPause();

        stopSlideshow();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
        if (slideThread != null) {
            slideThread.interrupt();
            slideThread = null;
        }
    }

    private void startSlideshow() {
        slideThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(SLIDESHOW_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            currentItem = (currentItem + 1) % IMAGES.length;
                            imageView.setImageResource(IMAGES[currentItem]);
                        }
                    });
                }
            }
        });
        slideThread.start();
    }

    private void stopSlideshow() {
        if (slideThread != null) {
            slideThread.interrupt();
            slideThread = null;
        }
    }

    private void setupListView() {
        mDatabase = FirebaseDatabase.getInstance("https://albatross-ed1d1-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();
        DatabaseReference idRef = mDatabase.child("ID");
        Query query = idRef.limitToFirst(5);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                items = new ArrayList<>();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    ids.add(childSnapshot.getKey());
                    HashMap<String, String> idValue = (HashMap<String, String>) childSnapshot.getValue();
                    employerIdTokens.add(idValue.get("employerIdToken"));
                    items.add(idValue.get("name") + "\n" + "시급 " + idValue.get("wage") + "원\n" + idValue.get("startHour") + "시 ~ " + idValue.get("endHour") + "시\n" + "경기도 수원시" + idValue.get("region") + "\n" + idValue.get("phoneNumber"));
                }
                adapter = new ListAdapter(mActivity, items);
                adapter.setFilteredList(items);
                setListAdapter(adapter);

                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        filterList(editable.toString());
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Error: " + databaseError.getMessage());
            }
        });
    }

    private void filterList(String searchText) {
        ListAdapter adapter = (ListAdapter) getListAdapter();
        adapter.getFilter().filter(searchText);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_ITEM, currentItem);

        // 리스트 뷰의 스크롤 위치를 저장합니다.
        ListView listView = getListView();
        outState.putParcelable(KEY_LIST_STATE, listView.onSaveInstanceState());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        imageView = rootView.findViewById(R.id.image1);

        setupListView();


        return rootView;
        //return inflater.inflate(R.layout.fragment_home, container, false);
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        // 클릭된 아이템의 값을 가져옵니다.
        String item = (String) getListAdapter().getItem(position);
        // Toast로 출력합니다.
        //Toast.makeText(getActivity(), "선택된 알바: " + item, Toast.LENGTH_SHORT).show();
        Intent showDetail = new Intent(mActivity.getApplicationContext(), DetailActivity.class);
        showDetail.putExtra("jobId", item);
        showDetail.putExtra("employerIdToken", employerIdTokens.get(position));
        showDetail.putExtra("id", ids.get(position));
        startActivity(showDetail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editText = view.findViewById(R.id.search_view);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 구현 내용 없음.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 입력된 텍스트에 기반하여 항목을 필터링합니다.
                filterItems(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 구현 내용 없음.
            }
        });
    }

    private void filterItems(String searchText) {
        filteredItems = new ArrayList<>();

        String[] searchWords = searchText.toLowerCase().split("\\s+");

        for (String item : items) {
            boolean isMatched = true;
            for (String word : searchWords) {
                if (!item.toLowerCase().contains(word)) {
                    isMatched = false;
                    break;
                }
            }
            if (isMatched || searchText.isEmpty()) {
                filteredItems.add(item);
            }
        }
        adapter.setFilteredList(filteredItems);
    }

}