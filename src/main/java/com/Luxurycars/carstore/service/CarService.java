
package com.Luxurycars.carstore.service;
import com.Luxurycars.carstore.exception.ResourceNotFoundException;
import com.Luxurycars.carstore.dto.CarMapper;
import com.Luxurycars.carstore.dto.CarStatsDTO;
import com.Luxurycars.carstore.dto.PageResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import com.Luxurycars.carstore.dto.CarRequestDTO;
import com.Luxurycars.carstore.dto.CarResponseDTO;
import com.Luxurycars.carstore.entity.Car;
import com.Luxurycars.carstore.repository.CarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import com.Luxurycars.carstore.config.CacheConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import com.github.benmanes.caffeine.cache.Cache;

@Service
public class CarService {

    private static final Logger log = LoggerFactory.getLogger(CarService.class);

    private final CarRepository carRepository;
    private final FileStorageService fileStorageService;
    private final CacheManager cacheManager;

    @Autowired
    public CarService(CarRepository carRepository,
                      FileStorageService fileStorageService,
                      CacheManager cacheManager) {
        this.carRepository = carRepository;
        this.fileStorageService = fileStorageService;
        this.cacheManager = cacheManager;
    }

    // ─── STATS ───
    @Cacheable(value = CacheConfig.CARS_STATS_CACHE)
    public CarStatsDTO getStats() {
        log.debug("DB HIT: getStats");
        List<Car> cars = carRepository.findAll();

        long total = cars.size();
        long withImages = cars.stream().filter(c -> c.getImagePath() != null).count();

        BigDecimal totalValue = cars.stream()
                .map(Car::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgPrice = total > 0
                ? totalValue.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal maxPrice = cars.stream()
                .map(Car::getPrice)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        BigDecimal minPrice = cars.stream()
                .map(Car::getPrice)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        Map<String, Long> byBrand = cars.stream()
                .collect(Collectors.groupingBy(Car::getBrand, Collectors.counting()));

        Map<String, Long> byLocation = cars.stream()
                .collect(Collectors.groupingBy(Car::getShowroomLocation, Collectors.counting()));

        return CarStatsDTO.builder()
                .totalCars(total)
                .carsWithImages(withImages)
                .totalInventoryValue(totalValue)
                .averagePrice(avgPrice)
                .maxPrice(maxPrice)
                .minPrice(minPrice)
                .carsByBrand(byBrand)
                .carsByLocation(byLocation)
                .build();
    }

    // ─── FEATURED (5 most expensive) ───
    // ─── FEATURED ───
    @Cacheable(value = CacheConfig.FEATURED_CACHE)
    public List<CarResponseDTO> getFeaturedCars() {
        log.debug("DB HIT: getFeaturedCars");
        // ... existing logic
        return carRepository.findAll().stream()
                .sorted(Comparator.comparing(Car::getPrice).reversed())
                .limit(5)
                .map(CarMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ─── LATEST (5 most recently added) ───
    @Cacheable(value = CacheConfig.LATEST_CACHE)
    public List<CarResponseDTO> getLatestCars() {
        log.debug("DB HIT: getLatestCars");
        return carRepository.findAll().stream()
                .sorted(Comparator.comparing(Car::getCreatedAt).reversed())
                .limit(5)
                .map(CarMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ─── GET ALL CARS (returns DTOs) ───
    public List<CarResponseDTO> getAllCars() {
        return carRepository.findAll()
                .stream()
                .map(CarMapper::toResponseDTO)   // convert each Car → DTO
                .collect(Collectors.toList());
    }

    // ─── GET ONE CAR BY ID (returns DTO) ───
    // ─── GET ONE CAR ───
    @Cacheable(value = CacheConfig.CAR_CACHE, key = "#id")
    public CarResponseDTO getCarById(Long id) {
        log.debug("DB HIT: getCarById({})", id);
        Car car = findCarOrThrow(id);
        return CarMapper.toResponseDTO(car);
    }

    // ─── CREATE CAR (accepts DTO) ───
    @CacheEvict(value = {
            CacheConfig.CARS_CACHE,
            CacheConfig.CARS_STATS_CACHE,
            CacheConfig.LATEST_CACHE
    }, allEntries = true)
    public CarResponseDTO createCar(CarRequestDTO dto) {
        log.info("Creating new car: {}", dto.getName());
        Car car = CarMapper.toEntity(dto);
        Car saved = carRepository.save(car);
        return CarMapper.toResponseDTO(saved);
    }

    // ─── UPDATE CAR (accepts DTO) ───
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CAR_CACHE, key = "#id"),
            @CacheEvict(value = CacheConfig.CARS_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.CARS_STATS_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.FEATURED_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.LATEST_CACHE, allEntries = true)
    })
    public CarResponseDTO updateCar(Long id, CarRequestDTO dto) {
        log.info("Updating car: {}", id);
        Car existing = findCarOrThrow(id);

        existing.setName(dto.getName());
        existing.setBrand(dto.getBrand());
        existing.setPrice(dto.getPrice());
        existing.setDescription(dto.getDescription());
        existing.setColorOptions(dto.getColorOptions());
        existing.setShowroomLocation(dto.getShowroomLocation());
        existing.setDeliveryDays(dto.getDeliveryDays());
        existing.setPaymentOptions(dto.getPaymentOptions());

        Car updated = carRepository.save(existing);
        return CarMapper.toResponseDTO(updated);
    }

    // ─── DELETE ───
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CAR_CACHE, key = "#id"),
            @CacheEvict(value = CacheConfig.CARS_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.CARS_STATS_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.FEATURED_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.LATEST_CACHE, allEntries = true)
    })
    public void deleteCar(Long id) {
        log.info("Deleting car: {}", id);
        Car car = findCarOrThrow(id);
        if (car.getImagePath() != null) {
            fileStorageService.deleteFile(car.getImagePath());
        }
        carRepository.delete(car);
    }

    // ─── SEARCH & FILTER (return DTOs) ───
    public List<CarResponseDTO> searchCars(String keyword) {
        return toDTOList(carRepository
                .findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(keyword, keyword));
    }

    public List<CarResponseDTO> getCarsByBrand(String brand) {
        return toDTOList(carRepository.findByBrandIgnoreCase(brand));
    }

    public List<CarResponseDTO> getCarsByPriceRange(BigDecimal min, BigDecimal max) {
        return toDTOList(carRepository.findByPriceBetween(min, max));
    }

    public List<CarResponseDTO> getCarsByLocation(String location) {
        return toDTOList(carRepository.findByShowroomLocationIgnoreCase(location));
    }

    // ─── IMAGE UPLOAD (same as before, returns DTO) ───
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CAR_CACHE, key = "#id"),
            @CacheEvict(value = CacheConfig.CARS_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.CARS_STATS_CACHE, allEntries = true)
    })
    public CarResponseDTO uploadImage(Long id, MultipartFile file) throws IOException {
        log.info("Uploading image for car: {}", id);
        Car car = findCarOrThrow(id);

        // Delete old file if exists
        if (car.getImagePath() != null) {
            fileStorageService.deleteFile(car.getImagePath());
        }

        // Save new file to disk
        String filename = fileStorageService.saveFile(file, id);

        // Update DB with new path
        car.setImagePath(filename);
        car.setImageName(file.getOriginalFilename());
        car.setImageType(file.getContentType());

        Car saved = carRepository.save(car);
        return CarMapper.toResponseDTO(saved);
    }

    // ─── IMAGE FETCH (returns raw bytes) ───
    public byte[] getCarImage(Long id) {
        Car car = findCarOrThrow(id);
        if (car.getImagePath() == null) {
            return null;
        }
        return fileStorageService.loadFile(car.getImagePath());
    }

    // ─── INTERNAL HELPERS ───
    private Car findCarOrThrow(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + id));
    }

    private List<CarResponseDTO> toDTOList(List<Car> cars) {
        return cars.stream()
                .map(CarMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ─── GET CARS WITH PAGINATION + SORTING ───
    @Cacheable(
            value = CacheConfig.CARS_CACHE,
            key = "#page + '-' + #size + '-' + #sortBy + '-' + #direction"
    )
    public PageResponseDTO<CarResponseDTO> getCarsPaginated(
            int page, int size, String sortBy, String direction) {
        log.debug("DB HIT: getCarsPaginated({}, {}, {}, {})",
                page, size, sortBy, direction);

        // Build the sort object
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        // Build the pageable (page index, page size, sort)
        Pageable pageable = PageRequest.of(page, size, sort);

        // Fetch page from DB
        Page<Car> carPage = carRepository.findAll(pageable);
        Page<CarResponseDTO> dtoPage = carPage.map(CarMapper::toResponseDTO);

        // Wrap in our clean PageResponseDTO
        return PageResponseDTO.from(dtoPage);
    }

    public Map<String, Object> getCacheStats() {
        if (!(cacheManager instanceof CaffeineCacheManager cm)) {
            return java.util.Collections.emptyMap();
        }
        Map<String, Object> stats = new java.util.HashMap<>();
        for (String name : cm.getCacheNames()) {
            org.springframework.cache.Cache cache = cm.getCache(name);
            if (cache != null && cache.getNativeCache() instanceof Cache<?, ?> nativeCache) {
                var s = nativeCache.stats();
                Map<String, Object> details = new java.util.HashMap<>();
                details.put("hitCount", s.hitCount());
                details.put("missCount", s.missCount());
                details.put("hitRate", s.hitRate());
                details.put("missRate", s.missRate());
                details.put("requestCount", s.requestCount());
                details.put("evictionCount", s.evictionCount());
                details.put("summary", s.toString());
                stats.put(name, details);
            }
        }
        return stats;
    }
    }