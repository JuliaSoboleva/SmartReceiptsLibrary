package co.smartreceipts.android.settings.widget.editors.categories;

import co.smartreceipts.android.model.Category;
import co.smartreceipts.android.settings.widget.editors.EditableItemListener;
import co.smartreceipts.android.settings.widget.editors.adapters.DraggableEditableCardsAdapter;

public class CategoriesAdapter extends DraggableEditableCardsAdapter<Category> {

    public CategoriesAdapter(EditableItemListener<Category> listener) {
        super(listener);
    }

    @Override
    public void onBindViewHolder(EditableCardsViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Category category = items.get(position);

        holder.title.setText(category.getName());
        holder.summary.setText(category.getCode());

        holder.edit.setOnClickListener(v -> listener.onEditItem(category));
        holder.delete.setOnClickListener(v -> listener.onDeleteItem(category));
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }
}