package com.example.duebuddy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NotificationAdapter
        extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification);
    }

    public interface OnNotificationDeleteListener {
        void onNotificationDelete(AppNotification notification);
    }

    private final ArrayList<AppNotification> notifications;
    private final OnNotificationClickListener clickListener;
    private final OnNotificationDeleteListener deleteListener;

    public NotificationAdapter(
            ArrayList<AppNotification> notifications,
            OnNotificationClickListener clickListener,
            OnNotificationDeleteListener deleteListener
    ) {
        this.notifications = notifications;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_notification,
                        parent,
                        false
                );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        AppNotification notification = notifications.get(position);

        holder.title.setText(notification.getTitle());
        holder.message.setText(notification.getMessage());
        holder.time.setText(formatTime(notification.getCreatedAt()));

        holder.unread.setVisibility(
                notification.isRead()
                        ? View.GONE
                        : View.VISIBLE
        );

        holder.itemView.setOnClickListener(
                v -> clickListener.onNotificationClick(notification)
        );

        holder.deleteButton.setOnClickListener(
                v -> deleteListener.onNotificationDelete(notification)
        );
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    private String formatTime(long time) {
        return new SimpleDateFormat(
                "MMM d, yyyy • h:mm a",
                Locale.getDefault()
        ).format(new Date(time));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        TextView message;
        TextView time;
        View unread;
        ImageButton deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(
                    R.id.txtNotificationTitle
            );

            message = itemView.findViewById(
                    R.id.txtNotificationMessage
            );

            time = itemView.findViewById(
                    R.id.txtNotificationTime
            );

            unread = itemView.findViewById(
                    R.id.viewUnread
            );

            deleteButton = itemView.findViewById(
                    R.id.btnDeleteNotification
            );
        }
    }
}
