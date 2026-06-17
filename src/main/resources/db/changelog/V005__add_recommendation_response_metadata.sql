--liquibase formatted sql

--changeset evready:005-add-recommendation-run-missing-information-json
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'recommendation_run' AND column_name = 'missing_information_json';
ALTER TABLE recommendation_run
    ADD COLUMN missing_information_json JSONB;

--changeset evready:005-add-recommendation-run-warnings-json
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'recommendation_run' AND column_name = 'warnings_json';
ALTER TABLE recommendation_run
    ADD COLUMN warnings_json JSONB;