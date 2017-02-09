package com.meridian.dacp.service;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class FadeService {
    
    private final Logger logger = LoggerFactory.getLogger(FadeService.class);
    
    private final DacpService dacpService;
    
    @Autowired
    public FadeService(DacpService dacpService) {
        this.dacpService = dacpService;
    }
    
    @Async
    public void fade(String id, double startVolume, double endVolume, double duration) {
        long start = System.currentTimeMillis();
        double totalChange = endVolume - startVolume;
        double durationMillis = duration * 1000;
        double currentVol = startVolume;
        while (currentVol < endVolume) {
            long elapsed = System.currentTimeMillis() - start;
            double percentElapsed = Math.min(elapsed / durationMillis, 1.0);
            currentVol = totalChange * percentElapsed;
            
            logger.info("Setting volume of {} to {} as part of {} second fade", id, currentVol, duration);
            // Set current volume of speaker
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
    }

}
