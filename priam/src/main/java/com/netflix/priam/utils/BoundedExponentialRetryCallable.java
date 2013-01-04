package com.netflix.priam.utils;

import java.util.concurrent.CancellationException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BoundedExponentialRetryCallable<T> extends RetryableCallable<T>
{    
    public final static long MAX_SLEEP = 10000;
    public final static long MIN_SLEEP = 1000;
    public final static int MAX_RETRIES = 3600;

    private static final Logger logger = LoggerFactory.getLogger(BoundedExponentialRetryCallable.class);
    private long max;
    private long min;
    private int maxRetries;
    private final ThreadSleeper sleeper = new ThreadSleeper();
    
    public BoundedExponentialRetryCallable()
    {
        this.max = MAX_SLEEP;
        this.min = MIN_SLEEP;
        this.maxRetries = MAX_RETRIES;
    }

    public BoundedExponentialRetryCallable(long minSleep, long maxSleep, int maxNumRetries)
    {
        this.max = maxSleep;
        this.min = minSleep;
        this.maxRetries = maxNumRetries;
    }

    public T call() throws Exception
    {
        long delay = min;// ms
        int retry = 0;
        int logCounter = 0;
        while (true)
        {
            try
            {
                return retriableCall();
            }
            catch (CancellationException e)
            {
                throw e;
            }
            catch (Exception e)
            {                
            		retry++;
            		
            		if (delay < max && retry <= maxRetries)
                {
            			delay *= 2;
                    logger.error(String.format("Retry #%d for: %s",retry, e.getMessage()));
                    if(++logCounter == 1)
                       logger.info("Exception --> "+ExceptionUtils.getFullStackTrace(e));
                    sleeper.sleep(delay);            			
                }
            		else if(delay >= max && retry <= maxRetries)
            		{
            			logger.error(String.format("Retry #%d for: %s",retry, e.getMessage()));
            			sleeper.sleep(max); 
            		}
            		else
            		{
            			throw e;
            		}
            }
            finally
            {
                forEachExecution();
            }
        }
    }

}