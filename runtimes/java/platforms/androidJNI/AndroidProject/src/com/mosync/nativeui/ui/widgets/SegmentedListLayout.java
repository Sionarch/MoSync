/* Copyright (C) 2012 MoSync AB

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License,
version 2, as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
MA 02110-1301, USA.
*/

package com.mosync.nativeui.ui.widgets;

import java.util.ArrayList;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.mosync.internal.android.EventQueue;
import com.mosync.nativeui.core.Types;
import com.mosync.nativeui.ui.widgets.SegmentedListViewSection.AdapterChangedListener;
import com.mosync.nativeui.util.properties.InvalidPropertyValueException;
import com.mosync.nativeui.util.properties.PropertyConversionException;

/**
 * A Segmented list is a container for SegmentedListSections.
 *
 * @author emma
 */
public class SegmentedListLayout extends ListLayout
	implements AdapterChangedListener
{

	public static final int ITEM_VIEW_TYPE_ITEM = 0;
	public static final int ITEM_VIEW_TYPE_HEADER = 1;
	public static final int ITEM_VIEW_TYPE_FOOTER = 2;
	public static final int ITEM_VIEW_TYPE_COUNT = 3;

	/**
	 * Feeds the list with views.
	 */
	private ArrayList<SegmentedListViewSection> mSections =
			new ArrayList<SegmentedListViewSection>();

	/**
	 * Constructor.
	 *
	 * @param handle Integer handle corresponding to this instance.
	 * @param listView A list wrapped by this widget.
	 */
	public SegmentedListLayout(int handle, Activity activity, ListView listView)
	{
		super( handle, listView );
		m_viewAdapter = new SegmentedViewAdapter( );
		listView.setAdapter( m_viewAdapter );
		listView.setOnItemClickListener( new SegmentedListOnItemClickListener( handle ) );
	}

	/**
	 * Class responsible for sending a mosync event when an item
	 * in the segmented list has been clicked.
	 *
	 * @author emma
	 */
	public class SegmentedListOnItemClickListener implements OnItemClickListener
	{
		private int m_handle = -1;

		public SegmentedListOnItemClickListener(int handle)
		{
			m_handle = handle;
		}

		/**
		 * @see OnItemClickListener.onItemClick.
		 */
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			int browseIndex = 0;
			int sectionIndex = 0;
			// Section's start position in list.
			int sectionPosition = 0;

			// Get the index of the section that holds this item,
			// and it's start position.
			for (int i=0; i < mSections.size(); i++)
			{
				SegmentedListViewSection section = mSections.get(i);
				browseIndex += section.itemsCount();
				sectionIndex = i;
				if ( browseIndex > position )
				{
					// The index ignores the header.
					EventQueue.getDefault().
							postSegmentedListItemClicked(m_handle, sectionIndex, position-sectionPosition-1);
					return;
				}
				sectionPosition += section.itemsCount();
			}
			EventQueue.getDefault().
					postSegmentedListItemClicked(m_handle, sectionIndex, position-sectionPosition-1);
		}
	}

	/**
	 * Add the child to the view adapter rather to
	 * the list view itself.
	 */
	@Override
	public void addChildAt(Widget child, int index)
	{
		if ( !(child instanceof SegmentedListViewSection) )
			return;

		SegmentedListViewSection section = (SegmentedListViewSection) child;
		section.setParent(this);
		m_children.add(section);
		mSections.add(section);

		int listIndex = index;
		if( index == -1 )
		{
			listIndex = m_viewAdapter.getCount( );
		}

		// Get all items from section and add them to the list adapter.
		for (int i=0; i < section.itemsCount(); i++)
		{
			ListItemWidget item = section.getItem(i);
			m_viewAdapter.addAt(item.getRootView(), listIndex + i);

			updateLayoutParamsForChild(item);

			ViewGroup layout = getView( );
			layout.addView( item.getRootView( ), listIndex+i );
		}

		// Set adapter listeners for each section.
		section.setAdapterListener(this);
	}

	@Override
	public void itemRemoved(ListItemWidget item)
	{
		m_viewAdapter.remove(item.getRootView());
		m_viewAdapter.notifyDataSetChanged();
//		ViewGroup layout = getView();
//		layout.removeView(item.getRootView());
	}

	@Override
	public void itemAdded(
			ListItemWidget item, SegmentedListViewSection section, int index)
	{
		// Based on index inside the section, compute the position in list.
		int itemPos = getItemPosition(mSections.indexOf(section), index);
		m_viewAdapter.addAt(item.getRootView(), itemPos);
		updateLayoutParamsForChild(item);
		ViewGroup layout = getView( );
		layout.addView( item.getRootView( ), itemPos );
	}

	@Override
	public void removeChild(Widget child)
	{
		// Do not call super.removeChild because section is not a widget.

		SegmentedListViewSection section = (SegmentedListViewSection) child;
		for (int i=0; i < section.itemsCount(); i++)
		{
			// Section is removed from list, it's items removed from adapter.
			m_viewAdapter.remove(section.getItem(i).getRootView());
//			ViewGroup layout = getView();
//			layout.removeView(section.getItem(i).getRootView());
		}
		// Nullify adapter listener.
		section.setAdapterListener(null);

		m_children.remove(child);
		section.setParent(null);

		mSections.remove(section);
		m_viewAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean setProperty(String property, String value)
			throws PropertyConversionException, InvalidPropertyValueException
	{
		// Reversed property is not available.
		if( super.setProperty( property, value ) &&
				!property.equals(Types.WIDGET_PROPERTY_REVERSED))
		{
			return true;
		}

		if( property.equals( "reload_data" ) )
		{
			m_viewAdapter.notifyDataSetChanged();

		}
		else
		{
			return false;
		}

		return true;
	}

	/**
	 * A custom adapter that feeds the list with views:
	 * standard list view items and separators for
	 * header and footer.
	 *
	 * @author emma
	 */
	class SegmentedViewAdapter extends ViewAdapter
	{
		public SegmentedViewAdapter()
		{
			super();
		}

		@Override
		public boolean isEnabled(int position) {
			// A separator cannot be clicked !
			return getItemViewType(position) == ITEM_VIEW_TYPE_ITEM;
		}

		@Override
		public int getItemViewType(int position) {
			ListItemWidget item = (ListItemWidget) getItem(position);
			return item.getItemType();
		}

		@Override
		public Object getItem(int position) {
			int browseIndex = 0;
			int sectionIndex = 0;
			// Section start position in list.
			int sectionPosition = 0;

			// Get the index of the section that holds this item,
			// and get it's start position.
			for (int i=0; i < mSections.size(); i++)
			{
				SegmentedListViewSection section = mSections.get(i);
				browseIndex += section.itemsCount();
				sectionIndex = i;
				if ( browseIndex > position )
				{
					return mSections.get(sectionIndex).getItem(position-sectionPosition);
				}
				sectionPosition += section.itemsCount();
			}
			return mSections.get(sectionIndex).getItem(position-sectionPosition);
		}
	}

	/**
	 * Helper function.
	 * Based on item position inside the section,
	 * return the position inside the list.
	 * @param sectionIndex The section index.
	 * @param index The index inside the section.
	 * @return The position inside the list.
	 */
	private int getItemPosition(int sectionIndex, int index)
	{
		int browseIndex = 0;
		if (sectionIndex == 0)
			return index;

		for (int i=1; i <= sectionIndex; i++)
		{
			browseIndex += mSections.get(i).itemsCount();
		}
		return browseIndex + index;
	}
}