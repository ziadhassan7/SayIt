package com.ziad.sayit.screens.LanguagesActivity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ziad.sayit.Helper;
import com.ziad.sayit.Interfaces.LanguageListItemListener;
import com.ziad.sayit.R;
import com.ziad.sayit.Utils.LanguageUtils.LanguageManagerUtil;
import com.ziad.sayit.Utils.LanguageUtils.LanguagesConstants;

public class LanguagesAdapter extends RecyclerView.Adapter <LanguagesAdapter.LanguagesViewHolder> {
    private TextView langName, downloadState;
    private ImageButton checkedItemBtn;
    private LanguageListItemListener mLanguageListItemListener;
    private Context mContext;

    public LanguagesAdapter(LanguageListItemListener languageListItemListener, Context context){
        mLanguageListItemListener = languageListItemListener;
        mContext = context;
    }



    class LanguagesViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public LanguagesViewHolder(View itemView){
            super(itemView);

            //Reference your views here!
            langName = itemView.findViewById(R.id.lang_name);
            downloadState = itemView.findViewById(R.id.textView);
            checkedItemBtn = itemView.findViewById(R.id.check_btn);

            itemView.setOnClickListener(this);
        }

        void bind(int listIndex) {
            langName.setText(LanguagesConstants.LANGUAGES_LIST[listIndex]); //set text to its corresponding language

            if (LanguageManagerUtil.chosenLanguageIndex == listIndex){
                checkedItemBtn.setVisibility(View.VISIBLE);

                if(!LanguageManagerUtil.isLanguageInstalled() && Helper.isNetworkAvailable(mContext)){
                    downloadState.setVisibility(View.VISIBLE);
                    downloadState.setText("Downloading...");
                }
                if(!LanguageManagerUtil.isLanguageInstalled()&& !Helper.isNetworkAvailable(mContext)){
                    downloadState.setVisibility(View.VISIBLE);
                    downloadState.setText("Not Installed");
                }
            }
        }

        @Override
        public void onClick(View view) {
            int  clickedItemPosition = getAdapterPosition();

            mLanguageListItemListener.onLanguageItemSelected(clickedItemPosition);
        }

    }




    @NonNull
    @Override
    public LanguagesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        //.inflate(int resources OR viewType, ViewGroup OR parent, boolean shouldItAttachToParentImmediately);
        View view = LayoutInflater.from(context).inflate(R.layout.language_item, parent, false);

        //pass the view to a ViewHolder object and return it
        return new LanguagesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LanguagesViewHolder holder, int position) {
        holder.bind(position);
        holder.setIsRecyclable(false); //Do not Recycle Items
    }



    @Override
    public int getItemCount() {
        return LanguagesConstants.LANGUAGES_LIST.length;
    }


}
