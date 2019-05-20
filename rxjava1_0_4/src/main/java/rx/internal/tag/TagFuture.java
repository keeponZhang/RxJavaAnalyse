package rx.internal.tag;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.functions.Action;

/**
 * createBy	 keepon
 */
public class TagFuture implements Future {
	private Future mFuture;
	private Action mAction;

	public  TagFuture(Future future, Action action){

		mFuture = future;
		mAction = action;
	}
	public  TagFuture(Future future){

		mFuture = future;
	}
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		System.out.println("TagFuture TagAction cancel  mAction ="+mAction);
		if(mFuture!=null){
			return mFuture.cancel(mayInterruptIfRunning);
		}
		return false;
	}

	@Override
	public boolean isCancelled() {
		if(mFuture!=null){
			return  mFuture.isCancelled();
		}
		return false;
	}

	@Override
	public boolean isDone() {
		if(mFuture!=null){
			return mFuture.isDone();
		}
		return false;
	}

	@Override
	public Object get() throws InterruptedException, ExecutionException {
		if(mFuture!=null){
			return mFuture.get();
		}
		return null;
	}

	@Override
	public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if(mFuture!=null){
			return mFuture.get(timeout,unit);
		}
		return null;
	}
}
