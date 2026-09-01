package com.example.duebuddy;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class BillAdapter extends RecyclerView.Adapter<BillAdapter.ViewHolder> {

    public interface OnBillUpdatedListener {
        void onBillUpdated();
    }

    private final ArrayList<Bill> bills;
    private final int userId;
    private final OnBillUpdatedListener listener;

    public BillAdapter(ArrayList<Bill> bills, int userId,
                       OnBillUpdatedListener listener) {
        this.bills = bills;
        this.userId = userId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bill, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Bill bill = bills.get(position);
        Context context = holder.itemView.getContext();

        holder.name.setText(bill.getBillName());
        holder.amount.setText(
                "₱ " + String.format(Locale.US, "%.2f", bill.getAmount())
        );
        holder.dueDate.setText("Due: " + bill.getDueDate());
        holder.accountHolder.setText("Account: " + bill.getAccountHolder());
        holder.category.setText(bill.getCategory());

        updateStatus(holder, bill);
        setupEditButton(holder, bill, context);
        setupPaidButton(holder, bill, context);
        setupDeleteButton(holder, bill, context);
    }

    private void updateStatus(ViewHolder holder, Bill bill) {
        String status = bill.getDisplayStatus();
        holder.status.setText(status.toUpperCase(Locale.US));

        if ("Paid".equals(status)) {
            holder.status.setBackgroundResource(R.drawable.status_paid);
            holder.paid.setVisibility(View.GONE);
        } else if ("Overdue".equals(status) || "Due Today".equals(status)) {
            holder.status.setBackgroundResource(R.drawable.status_overdue);
            holder.paid.setVisibility(View.VISIBLE);
        } else {
            holder.status.setBackgroundResource(R.drawable.status_unpaid);
            holder.paid.setVisibility(View.VISIBLE);
        }
    }

    private void setupEditButton(ViewHolder holder, Bill bill, Context context) {
        holder.edit.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddBill.class);
            intent.putExtra("bill_id", bill.getId());
            context.startActivity(intent);
        });
    }

    private void setupPaidButton(ViewHolder holder, Bill bill, Context context) {
        holder.paid.setOnClickListener(v -> {
            DatabaseHelper db = new DatabaseHelper(context);

            if (!db.markBillPaid(bill.getId(), userId)) {
                return;
            }

            ReminderScheduler.cancel(context, bill.getId());
            NotificationHelper.showPaidConfirmation(context, bill);
            bill.setStatus("Paid");
            notifyItemChanged(holder.getBindingAdapterPosition());

            if (listener != null) {
                listener.onBillUpdated();
            }

            Toast.makeText(
                    context,
                    "Bill marked as Paid!",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void setupDeleteButton(ViewHolder holder, Bill bill, Context context) {
        holder.delete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Bill")
                    .setMessage("Are you sure you want to delete this bill?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        DatabaseHelper db = new DatabaseHelper(context);

                        if (!db.deleteBill(bill.getId(), userId)) {
                            return;
                        }

                        ReminderScheduler.cancel(context, bill.getId());

                        int position = holder.getBindingAdapterPosition();

                        if (position != RecyclerView.NO_POSITION) {
                            bills.remove(position);
                            notifyItemRemoved(position);
                        }

                        if (listener != null) {
                            listener.onBillUpdated();
                        }

                        Toast.makeText(
                                context,
                                "Bill deleted successfully.",
                                Toast.LENGTH_SHORT
                        ).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return bills.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        TextView amount;
        TextView dueDate;
        TextView status;
        TextView accountHolder;
        TextView category;
        Button edit;
        Button delete;
        Button paid;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.txtBillName);
            amount = itemView.findViewById(R.id.txtAmount);
            dueDate = itemView.findViewById(R.id.txtDueDate);
            status = itemView.findViewById(R.id.txtStatus);
            accountHolder = itemView.findViewById(R.id.txtAccountHolder);
            category = itemView.findViewById(R.id.txtCategory);
            edit = itemView.findViewById(R.id.btnEdit);
            delete = itemView.findViewById(R.id.btnDelete);
            paid = itemView.findViewById(R.id.btnPaid);
        }
    }
}
