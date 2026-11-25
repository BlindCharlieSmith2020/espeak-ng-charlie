/*
 * Copyright (C) 2022 Beka Gozalishvili
 * Copyright (C) 2013 Reece H. Dunn
 * Copyright (C) 2026 Charlie Smith
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nz.charlie.espeakng.preference;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import nz.charlie.espeakng.CheckVoiceData;
import nz.charlie.espeakng.R;
import nz.charlie.espeakng.VariantScanner;
import nz.charlie.espeakng.VoiceFolder;
import nz.charlie.espeakng.VoiceSettings;
import nz.charlie.espeakng.VoiceVariant;

import java.io.File;
import java.util.List;

/**
 * Voice variant picker. Categories and entries are discovered at runtime from
 * the engine's voices/!v folder (issue #2376), plus any files already imported
 * from Documents/eSpeak NG Voices.
 */
public class VoiceVariantPreference extends DialogPreference {
    private Spinner mCategory;
    private Spinner mVariant;

    private int mCategoryIndex = 0;
    private int mVariantIndex = 0;
    private List<VariantScanner.Category> mCategories;

    static class ViewHolder {
        public TextView text;
    }

    private static class VariantData {
        private final String displayName;
        private final VoiceVariant variant;

        VariantData(String displayName, String variant) {
            this.displayName = displayName;
            this.variant = VoiceVariant.parseVoiceVariant(variant);
        }

        public String getDisplayName(Context context) {
            return displayName;
        }

        public VoiceVariant getVariant() {
            return variant;
        }
    }

    public class VariantDataListAdapter extends ArrayAdapter<VariantData> {
        private final LayoutInflater mInflater;

        public VariantDataListAdapter(Activity context, VariantData[] resources) {
            super(context, android.R.layout.simple_list_item_1, resources);
            mInflater = context.getLayoutInflater();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(android.R.id.text1);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.text.setText(getItem(position).getDisplayName(getContext()));
            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }
    }

    public class CategoryListAdapter extends ArrayAdapter<VariantScanner.Category> {
        private final LayoutInflater mInflater;

        public CategoryListAdapter(Activity context, List<VariantScanner.Category> categories) {
            super(context, android.R.layout.simple_list_item_1, categories);
            mInflater = context.getLayoutInflater();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(android.R.id.text1);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.text.setText(getItem(position).title);
            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }
    }

    public VoiceVariantPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setDialogLayoutResource(R.layout.voice_variant_preference);
        setLayoutResource(R.layout.information_view);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        reload();
    }

    public VoiceVariantPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VoiceVariantPreference(Context context) {
        this(context, null);
    }

    private void reload() {
        File data = CheckVoiceData.getDataPath(getContext());
        File engine = VoiceFolder.getEngineVariantsDir(data);
        File imported = VoiceFolder.getVariantsDir(VoiceFolder.getPublicRoot());
        mCategories = VariantScanner.scan(engine, imported);
        if (mCategories.isEmpty()) {
            mCategories = VariantScanner.buildCategories(null);
        }
    }

    private VariantData[] itemsOf(int category) {
        if (mCategories == null || category < 0 || category >= mCategories.size()) {
            return new VariantData[0];
        }
        List<VariantScanner.Item> items = mCategories.get(category).items;
        VariantData[] out = new VariantData[items.size()];
        for (int i = 0; i < items.size(); i++) {
            VariantScanner.Item item = items.get(i);
            out[i] = new VariantData(item.displayName, item.variantValue);
        }
        return out;
    }

    public void setVoiceVariant(VoiceVariant variant) {
        reload();
        for (int i = 0; i < mCategories.size(); ++i) {
            List<VariantScanner.Item> items = mCategories.get(i).items;
            for (int j = 0; j < items.size(); ++j) {
                VoiceVariant current = VoiceVariant.parseVoiceVariant(items.get(j).variantValue);
                if (current != null && current.equals(variant)) {
                    mCategoryIndex = i;
                    mVariantIndex = j;
                    onDataChanged();
                    return;
                }
            }
        }
        onDataChanged();
    }

    @Override
    protected View onCreateDialogView() {
        reload();
        View root = super.onCreateDialogView();
        mCategory = (Spinner) root.findViewById(R.id.category);
        mVariant = (Spinner) root.findViewById(R.id.variant);
        return root;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        final int category = mCategoryIndex;
        final int variant = mVariantIndex;

        mCategory.setAdapter(new CategoryListAdapter((Activity) getContext(), mCategories));
        mCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean mInitializing = true;

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mVariant.setAdapter(new VariantDataListAdapter((Activity) getContext(), itemsOf(position)));
                if (mInitializing) {
                    if (variant < mVariant.getCount()) {
                        mVariant.setSelection(variant);
                    }
                    mInitializing = false;
                }
                mCategoryIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        mVariant.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mVariantIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        if (category < mCategory.getCount()) {
            mCategory.setSelection(category);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                onDataChanged();
                if (shouldCommit()) {
                    SharedPreferences.Editor editor = getEditor();
                    if (editor != null) {
                        VariantData[] items = itemsOf(mCategoryIndex);
                        if (mVariantIndex >= 0 && mVariantIndex < items.length) {
                            VoiceVariant variant = items[mVariantIndex].getVariant();
                            editor.putString(VoiceSettings.PREF_VARIANT, variant.toString());
                            editor.commit();
                        }
                    }
                }
                break;
        }
        super.onClick(dialog, which);
    }

    private void onDataChanged() {
        if (mCategories == null || mCategories.isEmpty()) return;
        if (mCategoryIndex < 0 || mCategoryIndex >= mCategories.size()) return;
        VariantData[] items = itemsOf(mCategoryIndex);
        if (mVariantIndex < 0 || mVariantIndex >= items.length) return;
        String category = mCategories.get(mCategoryIndex).title;
        String variant = items[mVariantIndex].getDisplayName(getContext());
        callChangeListener(String.format("%s (%s)", category, variant));
    }
}
