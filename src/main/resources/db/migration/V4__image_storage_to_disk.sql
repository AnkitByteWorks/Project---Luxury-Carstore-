-- Replace BLOB storage with file path storage
ALTER TABLE cars DROP COLUMN image_data;
ALTER TABLE cars ADD COLUMN image_path VARCHAR(500);

-- Keep image_name and image_type for reference
-- (already in table from V1)