package com.example.dramebaz.shg.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.dramebaz.shg.adapter.FriendAdapter;
import com.example.dramebaz.shg.R;
import com.example.dramebaz.shg.RestApplication;
import com.example.dramebaz.shg.activity.ExpensesActivity;
import com.example.dramebaz.shg.client.SplitwiseRestClient;
import com.example.dramebaz.shg.splitwise.GroupMember;
import com.google.firebase.crash.FirebaseCrash;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {
    public static final String ARG_PAGE = "ARG_PAGE";
    private ArrayAdapter<GroupMember> friendAdapter;
    private List<GroupMember> friends;
    private int mPage;
    private SplitwiseRestClient client;
    private ListView lvFriends;
    private SwipeRefreshLayout swipeContainer;

    public FriendsFragment() {
        // Required empty public constructor
    }

    public static FriendsFragment newInstance(int page) {
        FriendsFragment fragment = new FriendsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPage = getArguments().getInt(ARG_PAGE);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.balance_per_contact_frnd, container, false);

        client = RestApplication.getSplitwiseRestClient();
        friends = new ArrayList<>();
        friendAdapter = new FriendAdapter(getContext(), friends);
        lvFriends = (ListView) view.findViewById(R.id.lvBalanceAll);
        lvFriends.setAdapter(friendAdapter);
        getFriendsList();

        lvFriends.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int friendId = friends.get(position).user.id;
                String balance = friends.get(position).balance.amount;
                String friendName = friends.get(position).user.firstName;
                Intent i = new Intent(getContext(), ExpensesActivity.class);
                i.putExtra(getResources().getString(R.string.type),getResources().getString(R.string.friend).toLowerCase());
                i.putExtra(getResources().getString(R.string.id), friendId);
                i.putExtra(getResources().getString(R.string.name), friendName);
                i.putExtra(getResources().getString(R.string.balance_key),balance);
                startActivity(i);
            }
        });

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                getFriendsList();
            }
        });

        return view;
    }


    private void getFriendsList(){
        client.getFriends(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject json) {
                try {
                    friendAdapter.clear();
                    Log.i(getResources().getString(R.string.get_friends), json.toString());
                    friends = GroupMember.fromJSONArray(json.getJSONArray(getResources().getString(R.string.friends)));
                    Button noFrnDataWarning = (Button) getActivity().findViewById(R.id.noFrnDataWarning);
                    if(friends.size()== 0){
                        noFrnDataWarning.setVisibility(View.VISIBLE);
                    }else {
                        noFrnDataWarning.setVisibility(View.INVISIBLE);
                    }

                    Log.i(getResources().getString(R.string.get_friends), friends.toString());
                    for (int i = 0; i<friends.size();i++){
                        GroupMember friend = friends.get(i);
                        if (mPage == 1 && friend.balance.amount != null && Double.valueOf(friend.balance.amount) < 0) {
                            friendAdapter.add(friend);
                        }
                        if (mPage == 2 && friend.balance.amount != null && Double.valueOf(friend.balance.amount) > 0){
                            friendAdapter.add(friend);
                        }
                        if(mPage == 0) {
                            friendAdapter.add(friend);
                        }

                    }
                    swipeContainer.setRefreshing(false);
                    friendAdapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    FirebaseCrash.report(e);
                    Log.e(getResources().getString(R.string.get_friends), getResources().getString(R.string.json_parsing), e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Toast.makeText(getContext(), getResources().getString(R.string.error_try_again),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

}
