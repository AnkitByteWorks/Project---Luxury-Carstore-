-- ═══════════════════════════════════════════════
-- V5: Seed High-Resolution Luxury Car Images
-- ═══════════════════════════════════════════════

-- Ensure image_url column exists in cars table
ALTER TABLE cars ADD COLUMN image_url VARCHAR(1000);

-- Update all existing vehicles with ultra-high-resolution Unsplash/CDN images
UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1563720223185-11003d516935?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%Phantom%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1592198084033-aade902d1aae?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%SF90%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1544829099-b9a0c07fad1a?w=1600&auto=format&fit=crop&q=80' WHERE (name ILIKE '%Revuelto%' OR name ILIKE '%Aventador%') AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1614162692292-7ac56d7f7f1e?w=1600&auto=format&fit=crop&q=80' WHERE (name ILIKE '%911%' OR name ILIKE '%GT3%') AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1603584173870-7f23fdae1b7a?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%DBS%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1580273916550-e323be2ae537?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%Continental%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1621135802920-133df287f89c?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%720S%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%AMG GT%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1603584173870-7f23fdae1b7a?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%R8%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1566008885218-90abf9200ddb?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%Chiron%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1555215695-3004980ad54e?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%M8%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1617788138017-80ad40651399?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%Model S%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1619682817481-e994891cd1f5?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%GT-R%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%F-Type%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1617814076367-b759c7d7e738?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%MC20%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%LC 500%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%Corvette%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1584345604476-8ec5e12e42dd?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%Mustang%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%Taycan%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');

UPDATE cars SET image_url = 'https://images.unsplash.com/photo-1544829099-b9a0c07fad1a?w=1600&auto=format&fit=crop&q=80' WHERE name ILIKE '%Jesko%' AND (image_url IS NULL OR image_url = '' OR image_url LIKE '%localhost%' OR image_url LIKE '%placeholder%');
