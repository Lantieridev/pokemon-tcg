-- V4__add_is_validated_to_chat_reports.sql
ALTER TABLE chat_reports ADD COLUMN is_validated BOOLEAN DEFAULT FALSE;
