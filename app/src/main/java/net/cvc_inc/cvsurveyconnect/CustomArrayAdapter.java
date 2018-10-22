package net.cvc_inc.cvsurveyconnect;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomArrayAdapter<T> extends BaseAdapter implements Filterable {

    private final Object mLock = new Object();
    private List<T> mObjects;
    private ArrayList<T> mOriginalValues;
    private Filter mFilter;
    private int mResource;
    private int mDropDownResource;
    private int mFieldId = 0;
    private boolean mNotifyOnChange = true;
    private Context mContext;
    private LayoutInflater mInflater;

    public CustomArrayAdapter(Context context, int resource, T[] objects) {
        init(context, resource, Arrays.asList(objects));
    }

    public CustomArrayAdapter(Context context, int resource, List objects) {
        init(context, resource, objects);
    }

    private void init(Context context, int resource, List<T> objects) {
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = mDropDownResource = resource;
        mObjects = objects;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        mNotifyOnChange = true;
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new CustomArrayAdapter.CustomArrayFilter();
        }
        return mFilter;
    }

    @Override
    public int getCount() {
        return mObjects.size();
    }

    @Override
    public T getItem(int position) {
        return mObjects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mResource);
    }

    private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource) {
        View view;
        TextView text;
        if (convertView == null) {
            view = mInflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }
        try {
            if (mFieldId == 0) {
                //  If no custom field is assigned, assume the whole resource is a TextView
                text = (TextView) view;
//                text.setTextSize(20);
//                text.setTypeface(georgia);
            } else {
                //  Otherwise, find the TextView field within the layout
                text = view.findViewById(mFieldId);
            }
        } catch (Exception e) {
            Log.d("CUSTOM_ADAPTER", "An error occurred in " + new Object() {
            }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
            throw new IllegalStateException(
                    "CustomArrayAdapter requires the resource ID to be a TextView", e);
        }
        T item = getItem(position);
        if (item instanceof CharSequence) {
            text.setText((CharSequence) item);
        } else {
            text.setText(item.toString());
        }
        text.setTextColor(Color.WHITE);
        return view;
    }

    private class CustomArrayFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence matchChars) {
            FilterResults results = new FilterResults();
            if (mOriginalValues == null) {
                synchronized (mLock) {
                    mOriginalValues = new ArrayList<T>(mObjects);
                }
            }
            if (matchChars == null || matchChars.length() == 0) {
                ArrayList<T> list;
                synchronized (mLock) {
                    list = new ArrayList<T>(mOriginalValues);
                }
                results.values = list;
                results.count = list.size();
            } else {
                String matchString = matchChars.toString().toLowerCase();
                ArrayList<T> values;
                synchronized (mLock) {
                    values = new ArrayList<T>(mOriginalValues);
                }
                final int count = values.size();
                final ArrayList<T> newValues = new ArrayList<T>();
                for (int i = 0; i < count; i++) {
                    final T value = values.get(i);
                    final String valueText = value.toString().toLowerCase();

                    if (valueText.contains(matchString)) {
                        newValues.add(value);
                    }
                }
                results.values = newValues;
                results.count = newValues.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mObjects = (List<T>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}