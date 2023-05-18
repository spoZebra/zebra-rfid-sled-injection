package com.zebra.eventinjectionservice;

interface IEventInjectionService {
    
    boolean authenticate();

    boolean injectInputEvent(in InputEvent event,int mode);

}