package com.eucalyptus.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.eucalyptus.records.Logs;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.Arrays;

public class Exceptions {
  private static Logger                    LOG                      = Logger.getLogger( Exceptions.class );
  private static final List<String>        DEFAULT_FILTER_MATCHES   = Lists.newArrayList( "com.eucalyptus", "edu.ucsb.eucalyptus" );
  private static final Integer             DEFAULT_FILTER_MAX_DEPTH = 10;
  private static final StackTraceElement[] steArrayType             = new StackTraceElement[1];
  
  enum ToString implements Function<Object, String> {
    INSTANCE;
    @Override
    public String apply( Object o ) {
      return ( o == null
        ? "null"
        : o.toString( ) );
    }
  };
  
  private static <T> Function<T, String> toStringFunction( ) {
    return ( Function<T, String> ) ToString.INSTANCE;
  }
  
  public static <T> Predicate<StackTraceElement> stackTraceElementFilter( final List<String> patterns ) {
    Function<StackTraceElement, String> toString = toStringFunction( );
    return Predicates.compose( makeSteFilter( patterns ), toString );
  }
  
  private static Predicate<String> makeSteFilter( final List<String> patterns ) {
    Predicate<String> filter = Predicates.alwaysTrue( );
    for ( String f : patterns ) {
      filter = Predicates.or( filter, Predicates.containsPattern( f ) );
    }
    return filter;
  }
  
  enum ExceptionCauses implements Function<Throwable, List<Throwable>> {
    INSTANCE;
    @Override
    public List<Throwable> apply( Throwable input ) {
      if ( input == null || input.getClass( ).equals( Exception.class )
           || input.getClass( ).equals( Exception.class )
           || input.getClass( ).equals( Exception.class ) ) {
        return Lists.newArrayList( );
      } else {
        List<Throwable> ret = Lists.newArrayList( input );
        ret.addAll( this.apply( input.getCause( ) ) );
        return ret;
      }
    }
  }
  
  public static <T extends Throwable> T maybeInterrupted( T t ) {
    if ( t instanceof InterruptedException ) {
      Thread.currentThread( ).interrupt( );
    }
    return t;
  }
  
  public static List<Throwable> causes( Throwable ex ) {
    return ExceptionCauses.INSTANCE.apply( ex );
  }
  
  /**
   * Convert this exception and all underlying causes, along with stack traces, into a string.
   * 
   * @param <T>
   * @param message
   * @param ex
   * @return
   */
  public static <T extends Throwable> String string( String message, T ex ) {
    return message + "\n"
           + string( ex );
  }
  
  /**
   * {@inheritDoc #string(String, Throwable)}
   */
  public static <T extends Throwable> String string( T ex ) {
    Throwable t = ( ex == null
      ? new RuntimeException( )
      : ex );
    String allMessages = causeString( ex );
    ByteArrayOutputStream os = new ByteArrayOutputStream( );
    PrintWriter p = new PrintWriter( os );
    p.println( allMessages );
    t.printStackTrace( p );
    p.flush( );
    for ( Throwable cause = t.getCause( ); cause != null; cause = cause.getCause( ) ) {
      p.print( "Caused by: " );
      cause.printStackTrace( p );
    }
    p.close( );
    return os.toString( );
  }
  
  public static <T extends Throwable> String causeString( T ex ) {
    return Joiner.on( "\nCaused by: " ).join( Exceptions.causes( ex ) );
  }
  
  /**
   * * Convert (possibly unwrapping) the argument {@link Throwable} into a suitable *
   * {@link RuntimeException} either by type casting or wrapping in an *
   * {@link UndeclaredThrowableException}. * * @param <T> * @param message * @param ex * @return
   */
  public static <T extends Throwable> RuntimeException toUndeclared( String message, T... exs ) {
    Throwable ex = null;
    if ( exs != null && exs.length > 0 ) {
      ex = exs[0];
    } else {
      ex = ( T ) new RuntimeException( message );
    }
    if ( ex instanceof UndeclaredThrowableException ) {
      return ( RuntimeException ) ex;
    } else if ( ex instanceof ExecutionException ) {
      return new UndeclaredThrowableException( ex.getCause( ), message );
    } else {
      return new UndeclaredThrowableException( ex, message );
    }
  }
  
  /** * {@inheritDoc #toUndeclared(String, Throwable)} * * @param cause * @return */
  public static RuntimeException toUndeclared( Throwable cause ) {
    return toUndeclared( cause.getMessage( ), cause );
  }
  
  public static <T extends Throwable> T filterStackTrace( T ex ) {
    ex.setStackTrace( Exceptions.filterStackTraceElements( ex, DEFAULT_FILTER_MATCHES )
                                .toArray( steArrayType ) );
    return ex;
  }
  
  public static Collection<StackTraceElement> filterStackTraceElements( Throwable ex ) {
    return Exceptions.filterStackTraceElements( ex, DEFAULT_FILTER_MATCHES );
  }
  
  private static Collection<StackTraceElement> filterStackTraceElements( Throwable ex, List<String> patterns ) {
    Predicate<StackTraceElement> filter = stackTraceElementFilter( patterns );
    return Collections2.filter( Arrays.asList( ex.getStackTrace( ) ), filter );
  }
  
  public static RuntimeException trace( String message ) {
    return trace( new RuntimeException( message ) );
  }
  
  public static <T extends Throwable> T trace( T t ) {
    return trace( t.getMessage( ), t );
  }
  
  public static <T extends Throwable> T trace( String message, T t ) {
    Throwable filtered = new RuntimeException( t.getMessage( ) );
    filtered.setStackTrace( Exceptions.filterStackTraceElements( t ).toArray( steArrayType ) );
    LOG.trace( message, filtered );
    return t;
  }
  
  public static <T extends Throwable> T error( String message ) {
    return ( T ) error( message, new RuntimeException( ) );
  }
  
  public static <T extends Throwable> T error( T t ) {
    return error( t.getMessage( ), t );
  }
  
  public static <T extends Throwable> T error( String message, T t ) {
    Throwable filtered = new RuntimeException( message );
    filtered.setStackTrace( Exceptions.filterStackTraceElements( t ).toArray( steArrayType ) );
    LOG.error( message, filtered );
    return t;
  }
  
  public static <T extends Throwable> boolean isCausedBy( Throwable ex, final Class<T> class1 ) {
    return findCause( ex, class1 ) != null;
  }
  
  @SuppressWarnings( "unchecked" )
  public static <T extends Throwable> T findCause( Throwable ex, final Class<T> class1 ) {
    try {
      return ( T ) Iterables.find( Exceptions.causes( ex ), new Predicate<Throwable>( ) {
                                     @Override
                                     public boolean apply( Throwable input ) {
                                       return class1.isAssignableFrom( input.getClass( ) );
                                     }
                                   } );
    } catch ( NoSuchElementException ex1 ) {
      return null;
    }
  }
  
  /** * @param message * @return */
  public static RuntimeException noSuchElement( String message, Throwable... t ) {
    if ( Logs.isExtrrreeeme( ) && t != null
         && t.length > 0 ) {
      return new NoSuchElementException( message + "\n"
                                         + string( message, t[0] ) );
    } else {
      return new NoSuchElementException( message );
    }
  }
  
}
