SELECT 
    v.type,
    v.source_system,
    v.source_txn_id,
    v.bank_account_id,
    ba.account_no AS bank_account_number,
    v.txn_ref,
    v.txn_date,
    v.amount,
    v.dr_cr_flag,
    v.description,
    v.is_mapped,
    v.created_at,
    NULL::integer AS status_id
FROM reconciliation.vw_all_bank_transactions v
LEFT JOIN reconciliation.bank_account ba ON ba.id = v.bank_account_id
WHERE 1=1
  AND COALESCE(v.is_mapped, FALSE) = FALSE
