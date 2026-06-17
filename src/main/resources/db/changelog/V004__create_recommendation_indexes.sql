--liquibase formatted sql

--changeset evready-recommender:V004-create-recommendation-run-created-at-index
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'idx_recommendation_run_created_at';
CREATE INDEX idx_recommendation_run_created_at
    ON recommendation_run (created_at DESC);

--changeset evready-recommender:V004-create-recommendation-candidate-snapshot-run-index
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'idx_recommendation_candidate_snapshot_run_id';
CREATE INDEX idx_recommendation_candidate_snapshot_run_id
    ON recommendation_candidate_snapshot (recommendation_run_id);

--changeset evready-recommender:V004-create-recommendation-candidate-snapshot-vehicle-index
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'idx_recommendation_candidate_snapshot_vehicle_id';
CREATE INDEX idx_recommendation_candidate_snapshot_vehicle_id
    ON recommendation_candidate_snapshot (vehicle_id);

--changeset evready-recommender:V004-create-recommendation-result-run-index
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'idx_recommendation_result_run_id';
CREATE INDEX idx_recommendation_result_run_id
    ON recommendation_result (recommendation_run_id);

--changeset evready-recommender:V004-create-recommendation-result-vehicle-index
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'idx_recommendation_result_vehicle_id';
CREATE INDEX idx_recommendation_result_vehicle_id
    ON recommendation_result (vehicle_id);