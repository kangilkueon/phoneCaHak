package com.tocotoucan.soft.phonecahak;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class StructureSelectFragment extends Fragment {
    Button structure_button1;
    Button structure_button2;
    Button structure_button3;
    Button structure_button4;
    Button structure_button5;

    private OnFragmentInteractionListener mListener;

    public StructureSelectFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.structure_select_fragment, container, false);
        structure_button1 = (Button) v.findViewById(R.id.structureButton1);
        structure_button1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mListener.onSelectStructureFrame(1);

                getActivity().getSupportFragmentManager().beginTransaction().remove(StructureSelectFragment.this).commit();
            }
        });

        structure_button2 = (Button) v.findViewById(R.id.structureButton2);
        structure_button2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mListener.onSelectStructureFrame(2);
                getActivity().getSupportFragmentManager().beginTransaction().remove(StructureSelectFragment.this).commit();
            }
        });

        structure_button3 = (Button) v.findViewById(R.id.structureButton3);
        structure_button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onSelectStructureFrame(3);
                getActivity().getSupportFragmentManager().beginTransaction().remove(StructureSelectFragment.this).commit();
            }
        });
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        public void onSelectStructureFrame(int structureType);
    }

}
