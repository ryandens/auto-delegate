package com.github.ryandens.examples;

import com.github.ryandens.delegation.AutoDelegate;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe {@link ExecutorService} implementation that counts the number of jobs that the
 * composed {@link ExecutorService} must execute
 */
@AutoDelegate(apisToDelegate = {ExecutorService.class})
public final class InstrumentedExecutorService extends AutoDelegate_InstrumentedExecutorService
    implements ExecutorService {

  private final AtomicInteger numJobs;

  public InstrumentedExecutorService(final ExecutorService inner) {
    super(inner);
    numJobs = new AtomicInteger();
  }

  /**
   * Note that we only increment {@link #numJobs} by one here as {@link
   * ExecutorService#invokeAny(Collection, long, TimeUnit)} only requires one {@link Callable} to
   * complete
   */
  @Override
  public <T> T invokeAny(
      final Collection<? extends Callable<T>> arg0, final long arg1, final TimeUnit arg2)
      throws InterruptedException, ExecutionException, TimeoutException {
    numJobs.getAndIncrement();
    return super.invokeAny(arg0, arg1, arg2);
  }

  /**
   * Note that we only increment {@link #numJobs} by one here as {@link
   * ExecutorService#invokeAny(Collection)} only requires one {@link Callable} to complete
   */
  @Override
  public <T> T invokeAny(final Collection<? extends Callable<T>> arg0)
      throws InterruptedException, ExecutionException {
    numJobs.getAndIncrement();
    return super.invokeAny(arg0);
  }

  @Override
  public Future<?> submit(final Runnable arg0) {
    numJobs.getAndIncrement();
    return super.submit(arg0);
  }

  @Override
  public void execute(final Runnable arg0) {
    numJobs.getAndIncrement();
    super.execute(arg0);
  }

  /** Note that we increment {@link #numJobs} by the size of the provided {@link Collection} */
  @Override
  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> arg0)
      throws InterruptedException {
    numJobs.addAndGet(arg0.size());
    return super.invokeAll(arg0);
  }

  /** Note that we increment {@link #numJobs} by the size of the provided {@link Collection} */
  @Override
  public <T> List<Future<T>> invokeAll(
      final Collection<? extends Callable<T>> arg0, final long arg1, final TimeUnit arg2)
      throws InterruptedException {
    numJobs.addAndGet(arg0.size());
    return super.invokeAll(arg0, arg1, arg2);
  }

  @Override
  public <T> Future<T> submit(final Callable<T> arg0) {
    numJobs.getAndIncrement();
    return super.submit(arg0);
  }

  @Override
  public <T> Future<T> submit(final Runnable arg0, final T arg1) {
    numJobs.getAndIncrement();
    return super.submit(arg0, arg1);
  }

  /**
   * @return the current number of jobs that the composed {@link ExecutorService} has been asked to
   *     do
   */
  public int getNumJobs() {
    return numJobs.get();
  }
}
