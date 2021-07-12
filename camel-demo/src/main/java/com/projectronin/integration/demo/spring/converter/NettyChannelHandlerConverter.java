package com.projectronin.integration.demo.spring.converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelHandler;

/**
 * Converter for changing comma-delimited String of bean names into a List of
 * Channel Handlers for supporting Netty.
 * 
 * @author Josh Smith
 */
@Component
public class NettyChannelHandlerConverter implements Converter<String, List<ChannelHandler>> {
	private ApplicationContext applicationContext;

	@Autowired
	public NettyChannelHandlerConverter(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public List<ChannelHandler> convert(String source) {
		return Arrays.stream(source.split(",")).filter(s -> s.startsWith("#"))
				.map(s -> applicationContext.getBean(s.substring(1), ChannelHandler.class))
				.collect(Collectors.toList());
	}

}
